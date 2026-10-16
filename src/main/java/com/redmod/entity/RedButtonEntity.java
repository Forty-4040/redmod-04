package com.redmod.entity;

import com.redmod.ModItems;
import com.redmod.ModSounds;
import com.redmod.item.RedButtonItem;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * Красная кнопка: маленькое существо, которое прыгает как слайм.
 * Анимации берутся из red_button.animation.json (GeckoLib).
 */
public class RedButtonEntity extends PathAwareEntity implements GeoEntity {

	// ===== Настройки (можно менять) =====

	/** Задержка между нажатиями в тиках: 3 тика = 0.15 секунды. */
	public static final int PRESS_COOLDOWN_TICKS = 3;
	/** Здоровье: 6 HP = 3 сердца. */
	public static final double MAX_HEALTH = 6.0;
	/** Сколько HP возвращает жареный стейк и морковка. */
	public static final float STEAK_HEAL = 4.0f;
	public static final float CARROT_HEAL = 2.0f;

	/** Тики «приседания» перед прыжком (под анимацию hop: отрыв на ~0.2 сек). */
	private static final int WINDUP_TICKS = 4;
	/** Сила прыжка вверх (0.42 как у игрока) и вперёд. */
	private static final double HOP_UP = 0.42;
	private static final double HOP_FORWARD = 0.20;
	private static final double HOP_FORWARD_FLEE = 0.27;
	/** Примерная дальность одного прыжка в блоках (для проверки безопасной посадки). */
	private static final double HOP_DISTANCE = 1.5;
	/** Радиус, в котором кнопка замечает мобов (убегает) и игрока (ходит рядом). */
	private static final double THREAT_RADIUS = 8.0;
	private static final double PLAYER_RADIUS = 24.0;
	/** Если игрок дальше этого расстояния, кнопка прыгает к нему. */
	private static final double FOLLOW_DISTANCE = 4.0;

	// ===== Анимации (названия из Blockbench) =====
	private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.red_button.idle");
	private static final RawAnimation ANIM_SUNK = RawAnimation.begin().thenPlayAndHold("animation.red_button.sunk");
	private static final RawAnimation ANIM_HOP = RawAnimation.begin().thenPlay("animation.red_button.hop");
	private static final RawAnimation ANIM_PRESS = RawAnimation.begin().thenPlay("animation.red_button.press");
	private static final RawAnimation ANIM_EAT = RawAnimation.begin().thenPlay("animation.red_button.eat");

	private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

	private long lastPressTick = -100;
	private int restTicks = 20;
	private int windupTicks = -1;
	private Vec3d pendingDirection = Vec3d.ZERO;
	private boolean pendingFlee = false;

	public RedButtonEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
		this.setPersistent(); // не исчезает, когда игрок далеко
		this.setEquipmentDropChance(EquipmentSlot.HEAD, 1.0f); // шлем всегда выпадает при смерти
	}

	public static DefaultAttributeContainer.Builder createRedButtonAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, MAX_HEALTH)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
	}

	// ===== Поведение: прыжки =====

	@Override
	protected void mobTick() {
		super.mobTick();

		// Вода: кнопка не плавает, тонет без движений.
		if (this.isTouchingWater()) {
			this.windupTicks = -1;
			Vec3d velocity = this.getVelocity();
			this.setVelocity(0.0, velocity.y, 0.0);
			return;
		}

		// В воздухе ничего не делаем, ждём приземления.
		if (!this.isOnGround()) {
			return;
		}

		// Приседание перед прыжком, потом прыжок.
		if (this.windupTicks >= 0) {
			if (this.windupTicks == 0) {
				this.launch();
				this.windupTicks = -1;
				this.restTicks = this.pendingFlee ? 5 + this.random.nextInt(10) : 20 + this.random.nextInt(40);
			} else {
				this.windupTicks--;
			}
			return;
		}

		// Пауза между прыжками.
		if (this.restTicks > 0) {
			this.restTicks--;
			return;
		}

		Vec3d direction = this.chooseDirection();
		if (direction == null) {
			this.restTicks = 20;
			return;
		}

		this.pendingDirection = direction;
		this.windupTicks = WINDUP_TICKS;
		this.faceDirection(direction);
		this.triggerAnim("action", "hop");
	}

	private void launch() {
		double speed = this.pendingFlee ? HOP_FORWARD_FLEE : HOP_FORWARD;
		this.setVelocity(this.pendingDirection.x * speed, HOP_UP, this.pendingDirection.z * speed);
	}

	private void faceDirection(Vec3d direction) {
		float yaw = (float) (MathHelper.atan2(direction.z, direction.x) * (180.0 / Math.PI)) - 90.0f;
		this.setYaw(yaw);
		this.setBodyYaw(yaw);
		this.setHeadYaw(yaw);
	}

	/** Выбирает направление прыжка или null, если прыгать не нужно. */
	private Vec3d chooseDirection() {
		this.pendingFlee = false;
		Vec3d desired = null;

		// 1. Убегаем от мобов-монстров.
		List<MobEntity> threats = this.getWorld().getEntitiesByClass(
				MobEntity.class,
				this.getBoundingBox().expand(THREAT_RADIUS),
				mob -> mob instanceof Monster && mob.isAlive()
		);
		if (!threats.isEmpty()) {
			Vec3d away = Vec3d.ZERO;
			for (MobEntity threat : threats) {
				Vec3d diff = this.getPos().subtract(threat.getPos());
				double length = diff.length();
				if (length < 0.01) {
					continue;
				}
				away = away.add(diff.normalize().multiply(1.0 / Math.max(length, 1.0)));
			}
			if (away.lengthSquared() > 1.0E-4) {
				desired = new Vec3d(away.x, 0.0, away.z).normalize();
				this.pendingFlee = true;
			}
		}

		// 2. Иначе ходим вокруг игрока.
		if (desired == null) {
			PlayerEntity player = this.getWorld().getClosestPlayer(this, PLAYER_RADIUS);
			if (player != null) {
				Vec3d toPlayer = player.getPos().subtract(this.getPos());
				toPlayer = new Vec3d(toPlayer.x, 0.0, toPlayer.z);
				double distance = toPlayer.length();
				if (distance > FOLLOW_DISTANCE) {
					desired = toPlayer.normalize();
				} else if (this.random.nextFloat() < 0.6f) {
					desired = this.randomDirection();
				}
			} else if (this.random.nextFloat() < 0.3f) {
				desired = this.randomDirection();
			}
		}

		if (desired == null) {
			return null;
		}

		// 3. Проверяем, что приземлимся в безопасном месте (не в воде, не с обрыва).
		for (int i = 0; i < 8; i++) {
			int step = (i + 1) / 2;
			double angle = Math.toRadians(45.0 * step) * (i % 2 == 1 ? 1 : -1);
			Vec3d candidate = i == 0 ? desired : rotateY(desired, angle);
			if (this.isSafeLanding(candidate)) {
				return candidate;
			}
		}
		return null;
	}

	private Vec3d randomDirection() {
		double angle = this.random.nextDouble() * Math.PI * 2.0;
		return new Vec3d(Math.cos(angle), 0.0, Math.sin(angle));
	}

	private static Vec3d rotateY(Vec3d v, double angle) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		return new Vec3d(v.x * cos - v.z * sin, 0.0, v.x * sin + v.z * cos);
	}

	/** Кнопка сама в воду не прыгает: проверяем клетку приземления. */
	private boolean isSafeLanding(Vec3d direction) {
		World world = this.getWorld();
		Vec3d landing = this.getPos().add(direction.multiply(HOP_DISTANCE));
		int x = MathHelper.floor(landing.x);
		int z = MathHelper.floor(landing.z);
		int baseY = MathHelper.floor(this.getY());

		for (int y = baseY + 1; y >= baseY - 3; y--) {
			BlockPos ground = new BlockPos(x, y, z);
			if (world.getBlockState(ground).getCollisionShape(world, ground).isEmpty()) {
				continue; // воздух, вода и т.п.: идём ниже
			}
			BlockPos stand = ground.up();
			boolean freeSpace = world.getBlockState(stand).getCollisionShape(world, stand).isEmpty();
			boolean noFluid = world.getFluidState(stand).isEmpty();
			return freeSpace && noFluid;
		}
		return false; // обрыв глубже 3 блоков
	}

	// ===== Взаимодействие =====

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);
		boolean client = this.getWorld().isClient;

		// Shift + ПКМ: взять кнопку в руки.
		if (player.isSneaking()) {
			if (hand != Hand.MAIN_HAND) {
				return ActionResult.PASS;
			}
			if (!client) {
				this.pickUp(player, hand);
			}
			return ActionResult.success(client);
		}

		// ПКМ едой: лечение (жареный стейк, морковка).
		float heal = healAmount(stack);
		if (heal > 0.0f) {
			if (this.getHealth() >= this.getMaxHealth()) {
				return ActionResult.CONSUME;
			}
			if (!client) {
				this.heal(heal);
				if (!player.isCreative()) {
					stack.decrement(1);
				}
				this.playSound(SoundEvents.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
				this.triggerAnim("action", "eat");
				if (this.getWorld() instanceof ServerWorld serverWorld) {
					serverWorld.spawnParticles(ParticleTypes.HEART, this.getX(), this.getY() + 0.3, this.getZ(), 5, 0.2, 0.1, 0.2, 0.02);
				}
			}
			return ActionResult.success(client);
		}

		// ПКМ шлемом: надеть шлем.
		if (isHelmet(stack)) {
			if (!client) {
				ItemStack old = this.getEquippedStack(EquipmentSlot.HEAD);
				if (!old.isEmpty()) {
					this.dropStack(old);
				}
				ItemStack one = stack.copy();
				one.setCount(1);
				this.equipStack(EquipmentSlot.HEAD, one);
				if (!player.isCreative()) {
					stack.decrement(1);
				}
				this.playSound(SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.0f);
			}
			return ActionResult.success(client);
		}

		// ПКМ пустой рукой: нажать.
		if (stack.isEmpty() && hand == Hand.MAIN_HAND) {
			if (!client) {
				this.press();
			}
			return ActionResult.success(client);
		}

		return super.interactMob(player, hand);
	}

	private static boolean isHelmet(ItemStack stack) {
		return stack.getItem() instanceof ArmorItem armor && armor.getType() == ArmorItem.Type.HELMET;
	}

	private static float healAmount(ItemStack stack) {
		if (stack.isOf(Items.COOKED_BEEF)) {
			return STEAK_HEAL;
		}
		if (stack.isOf(Items.CARROT)) {
			return CARROT_HEAL;
		}
		return 0.0f;
	}

	private void press() {
		long now = this.getWorld().getTime();
		if (now - this.lastPressTick < PRESS_COOLDOWN_TICKS) {
			return;
		}
		this.lastPressTick = now;
		this.playSound(ModSounds.RED_BUTTON_PRESS, 1.0f, 1.0f);
		this.triggerAnim("action", "press");
	}

	private void pickUp(PlayerEntity player, Hand hand) {
		ItemStack item = new ItemStack(this.getPickupItem());
		NbtCompound nbt = item.getOrCreateNbt();
		nbt.putFloat(RedButtonItem.HEALTH_KEY, this.getHealth());

		// Шлем (и факел у большой кнопки) при взятии в руки спадают.
		for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.MAINHAND}) {
			ItemStack worn = this.getEquippedStack(slot);
			if (!worn.isEmpty()) {
				this.dropStack(worn.copy());
				this.equipStack(slot, ItemStack.EMPTY);
			}
		}

		if (player.getStackInHand(hand).isEmpty()) {
			player.setStackInHand(hand, item);
		} else if (!player.getInventory().insertStack(item)) {
			player.dropItem(item, false);
		}
		this.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.6f, 1.2f);
		this.discard();
	}

	/** Какой предмет получает игрок, когда берёт существо в руки. */
	protected Item getPickupItem() {
		return ModItems.RED_BUTTON_ITEM;
	}

	// ===== Урон =====

	@Override
	public boolean damage(DamageSource source, float amount) {
		// Урона от падения нет.
		if (source.isIn(DamageTypeTags.IS_FALL)) {
			return false;
		}
		return super.damage(source, amount);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF;
	}

	// ===== GeckoLib: анимации =====

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		// Основная анимация: стоит (idle) или тонет (sunk).
		controllers.add(new AnimationController<>(this, "main", 3, this::mainPredicate));

		// Разовые анимации, которые запускает сервер: прыжок, нажатие, еда.
		controllers.add(new AnimationController<>(this, "action", 0, state -> PlayState.STOP)
				.triggerableAnim("hop", ANIM_HOP)
				.triggerableAnim("press", ANIM_PRESS)
				.triggerableAnim("eat", ANIM_EAT));
	}

	private PlayState mainPredicate(AnimationState<RedButtonEntity> state) {
		if (this.isTouchingWater()) {
			return state.setAndContinue(ANIM_SUNK);
		}
		return state.setAndContinue(ANIM_IDLE);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animationCache;
	}
}
