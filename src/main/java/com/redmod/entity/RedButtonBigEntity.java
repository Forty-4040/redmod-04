package com.redmod.entity;

import com.redmod.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * Большая красная кнопка: умеет всё то же, что и обычная (прыжки, шлем, нажатие, лечение),
 * но у неё 5 сердец и она может держать факел.
 * Факел лежит в основной руке существа, поэтому моды на динамический свет
 * (например, LambDynamicLights) заставляют кнопку светиться.
 */
public class RedButtonBigEntity extends RedButtonEntity {
	/** Здоровье: 10 HP = 5 сердец. */
	public static final double BIG_MAX_HEALTH = 10.0;

	public RedButtonBigEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
		this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 1.0f); // факел всегда выпадает при смерти
	}

	public static DefaultAttributeContainer.Builder createBigAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, BIG_MAX_HEALTH)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
	}

	@Override
	protected Item getPickupItem() {
		return ModItems.RED_BUTTON_BIG_ITEM;
	}

	private static boolean isTorch(ItemStack stack) {
		return stack.isOf(Items.TORCH) || stack.isOf(Items.SOUL_TORCH);
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);

		// ПКМ факелом: кнопка берёт факел в руку.
		if (!player.isSneaking() && isTorch(stack)) {
			boolean client = this.getWorld().isClient;
			if (!client) {
				ItemStack old = this.getEquippedStack(EquipmentSlot.MAINHAND);
				if (!old.isEmpty()) {
					this.dropStack(old.copy());
				}
				ItemStack one = stack.copy();
				one.setCount(1);
				this.equipStack(EquipmentSlot.MAINHAND, one);
				if (!player.isCreative()) {
					stack.decrement(1);
				}
				this.playSound(SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.0f);
			}
			return ActionResult.success(client);
		}

		return super.interactMob(player, hand);
	}
}
