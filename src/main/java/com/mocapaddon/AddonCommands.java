package com.mocapaddon;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * /mocap playback recording setting <inventorydrop|knockback> [true|false]
 * Registered as a second "mocap" tree; Brigadier merges it into Mocap's existing nodes.
 */
public final class AddonCommands {
	private AddonCommands() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection selection) {
		LiteralArgumentBuilder<CommandSourceStack> setting = Commands.literal("setting")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(toggle("inventorydrop", ModConfig::inventoryDrop, ModConfig::setInventoryDrop))
			.then(toggle("knockback", ModConfig::knockback, ModConfig::setKnockback));

		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("mocap")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal("playback")
				.then(Commands.literal("recording")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(setting)));
		dispatcher.register(root);
	}

	private static LiteralArgumentBuilder<CommandSourceStack> toggle(String name, BooleanSupplier getter, Consumer<Boolean> setter) {
		return Commands.literal(name)
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.executes(ctx -> {
				ctx.getSource().sendSuccess(() -> Component.literal("Mocap " + name + " is " + getter.getAsBoolean()), false);
				return 1;
			})
			.then(Commands.argument("value", BoolArgumentType.bool())
				.executes(ctx -> {
					boolean v = BoolArgumentType.getBool(ctx, "value");
					setter.accept(v);
					ctx.getSource().sendSuccess(() -> Component.literal("Mocap " + name + " set to " + v), false);
					return 1;
				}));
	}
}
