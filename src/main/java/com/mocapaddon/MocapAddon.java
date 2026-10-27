package com.mocapaddon;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MocapAddon implements ModInitializer {
	public static final String MOD_ID = "mocap_addon";
	public static final Logger LOGGER = LoggerFactory.getLogger("Mocap Addon");

	@Override
	public void onInitialize() {
		try {
			ModConfig.load();
			MocapCompat.verify();
			// Brigadier merges same-named literals, so order relative to Mocap does not matter.
			CommandRegistrationCallback.EVENT.register(AddonCommands::register);
			ServerTickEvents.END_SERVER_TICK.register(KnockbackHandler::tick);
			ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
				KnockbackHandler.clear();
				InventoryDropHandler.clear();
			});
		} catch (Throwable t) {
			LOGGER.warn("Mocap Addon failed to initialize; it will stay inactive.", t);
		}
	}
}
