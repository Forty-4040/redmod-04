package com.redmod;

import com.redmod.entity.RedButtonBigEntity;
import com.redmod.entity.RedButtonEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {

	// ===== Обычная красная кнопка =====

	// ВО СКОЛЬКО РАЗ УВЕЛИЧИТЬ КНОПКУ (1.0 = исходный размер модели, 2.0 = вдвое больше и т.д.).
	// Меняется только это число: модель, хитбокс, тень и шлем подстроятся сами.
	public static final float MODEL_SCALE = 2.0f;

	// Хитбокс при масштабе 1.0 (в блоках), дальше умножается на MODEL_SCALE.
	public static final float HITBOX_WIDTH = 0.3f * MODEL_SCALE;
	public static final float HITBOX_HEIGHT = 0.2f * MODEL_SCALE;

	// ===== Большая красная кнопка =====

	// Модель большой кнопки сама в 1.5 раза больше маленькой, поэтому масштаб у них одинаковый:
	// при изменении MODEL_SCALE растут обе. Если нужен другой размер большой, поменяй это число.
	public static final float BIG_MODEL_SCALE = MODEL_SCALE;

	// Хитбокс большой кнопки в блоках (ширина, высота), в 1.5 раза больше, чем у маленькой.
	public static final float BIG_HITBOX_WIDTH = 0.45f * BIG_MODEL_SCALE;
	public static final float BIG_HITBOX_HEIGHT = 0.3f * BIG_MODEL_SCALE;

	public static final EntityType<RedButtonEntity> RED_BUTTON = Registry.register(
			Registries.ENTITY_TYPE,
			new Identifier(RedMod.MOD_ID, "red_button"),
			FabricEntityTypeBuilder.<RedButtonEntity>create(SpawnGroup.MISC, RedButtonEntity::new)
					.dimensions(EntityDimensions.fixed(HITBOX_WIDTH, HITBOX_HEIGHT))
					.trackRangeChunks(8)
					.build()
	);

	public static final EntityType<RedButtonBigEntity> RED_BUTTON_BIG = Registry.register(
			Registries.ENTITY_TYPE,
			new Identifier(RedMod.MOD_ID, "red_button_big"),
			FabricEntityTypeBuilder.<RedButtonBigEntity>create(SpawnGroup.MISC, RedButtonBigEntity::new)
					.dimensions(EntityDimensions.fixed(BIG_HITBOX_WIDTH, BIG_HITBOX_HEIGHT))
					.trackRangeChunks(8)
					.build()
	);

	public static void init() {
		FabricDefaultAttributeRegistry.register(RED_BUTTON, RedButtonEntity.createRedButtonAttributes());
		FabricDefaultAttributeRegistry.register(RED_BUTTON_BIG, RedButtonBigEntity.createBigAttributes());
	}
}
