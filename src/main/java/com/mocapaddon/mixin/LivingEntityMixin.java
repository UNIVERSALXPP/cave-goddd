package com.mocapaddon.mixin;

import com.mocapaddon.InventoryDropHandler;
import com.mocapaddon.KnockbackHandler;
import com.mocapaddon.MocapAddon;
import com.mocapaddon.MocapCompat;
import com.mocapaddon.ModConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla-side hooks. Every hook returns immediately unless the entity is a Mocap FakePlayer. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Shadow
	protected abstract void dropEquipment(ServerLevel level);

	@Inject(method = "hurtServer", at = @At("HEAD"))
	private void mocapaddon$hurtHead(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		Object self = this;
		if (!MocapCompat.isFakePlayer(self)) return;
		LivingEntity le = (LivingEntity) self;
		if (!le.isDeadOrDying()) InventoryDropHandler.reset(le); // alive again (respawn) -> re-arm
		KnockbackHandler.suppress = KnockbackHandler.isReplayHurt(source);
	}

	@Inject(method = "hurtServer", at = @At("RETURN"))
	private void mocapaddon$hurtReturn(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		Object self = this;
		if (!MocapCompat.isFakePlayer(self)) return;
		KnockbackHandler.suppress = false;
		if (!ModConfig.inventoryDrop()) return;
		LivingEntity le = (LivingEntity) self;
		if (!le.isDeadOrDying()) return;
		if (le instanceof Player p && p.isSpectator()) return;
		if (MocapCompat.wasKilledByPlayback(self)) return; // Mocap's own fakeKill(): not a real kill
		if (!InventoryDropHandler.markDropped(le)) return;  // already dropped for this death
		try {
			// Vanilla Player.dropEquipment: respects keepInventory, drops main/hotbar/armor/offhand once
			// (inventory is emptied as it drops, so nothing is duplicated or left behind).
			this.dropEquipment(level);
		} catch (Throwable t) {
			MocapAddon.LOGGER.warn("Failed to drop inventory of Mocap playback player", t);
		}
	}

	@Inject(method = "knockback", at = @At("HEAD"))
	private void mocapaddon$kbHead(double strength, double x, double z, CallbackInfo ci) {
		Object self = this;
		if (KnockbackHandler.suppress || !ModConfig.knockback() || !MocapCompat.isFakePlayer(self)) return;
		try {
			KnockbackHandler.beforeKnockback((LivingEntity) self);
		} catch (Throwable t) {
			MocapAddon.LOGGER.warn("Knockback hook failed", t);
		}
	}

	@Inject(method = "knockback", at = @At("RETURN"))
	private void mocapaddon$kbReturn(double strength, double x, double z, CallbackInfo ci) {
		Object self = this;
		if (!MocapCompat.isFakePlayer(self)) return;
		try {
			KnockbackHandler.afterKnockback((LivingEntity) self);
		} catch (Throwable t) {
			MocapAddon.LOGGER.warn("Knockback hook failed", t);
		}
	}
}
