package com.mocapaddon.mixin.mocap;

import com.mocapaddon.KnockbackHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Targets Mocap by name (no compile-time dependency). Mocap's own members keep their real names,
 * hence remap = false. Lives in a non-required mixin config so a changed Mocap cannot crash the game.
 */
@Pseudo
@Mixin(targets = "net.mt1006.mocap.mocap.playing.playback.ActionContext", remap = false)
public abstract class ActionContextMixin {
	@Shadow(remap = false)
	public abstract Entity getEntity();

	@Inject(method = "changePosition", at = @At("TAIL"), remap = false, require = 0)
	private void mocapaddon$afterChangePosition(Vec3 newPos, float rotY, float rotX, boolean transformRot, CallbackInfo ci) {
		try {
			KnockbackHandler.onPathPosition(this.getEntity());
		} catch (Throwable ignored) {
			// never let the addon break Mocap playback
		}
	}
}
