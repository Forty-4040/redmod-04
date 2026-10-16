package com.redmod.client;

import com.redmod.RedMod;
import com.redmod.entity.RedButtonEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/**
 * Подключает файлы из Blockbench: модель, анимации и текстуру.
 * Анимации у обычной и большой кнопки общие (red_button.animation.json),
 * потому что в обеих моделях одинаковые названия костей (Body, helmet).
 */
public class RedButtonModel<T extends RedButtonEntity> extends GeoModel<T> {
	private final Identifier model;
	private final Identifier texture;
	private final Identifier animation;

	/** @param name имя файлов модели и текстуры: "red_button" или "red_button_big" */
	public RedButtonModel(String name) {
		this.model = new Identifier(RedMod.MOD_ID, "geo/entity/" + name + ".geo.json");
		this.texture = new Identifier(RedMod.MOD_ID, "textures/entity/" + name + ".png");
		this.animation = new Identifier(RedMod.MOD_ID, "animations/entity/red_button.animation.json");
	}

	@Override
	public Identifier getModelResource(T animatable) {
		return this.model;
	}

	@Override
	public Identifier getTextureResource(T animatable) {
		return this.texture;
	}

	@Override
	public Identifier getAnimationResource(T animatable) {
		return this.animation;
	}
}
