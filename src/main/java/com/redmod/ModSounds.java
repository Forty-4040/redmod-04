package com.redmod;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
	// Звук нажатия: assets/redmod/sounds/red_button_press.ogg (описан в sounds.json)
	public static final SoundEvent RED_BUTTON_PRESS = register("red_button_press");

	private static SoundEvent register(String name) {
		Identifier id = new Identifier(RedMod.MOD_ID, name);
		return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
	}

	public static void init() {
		// Вызов нужен, чтобы класс загрузился и звук зарегистрировался.
	}
}
