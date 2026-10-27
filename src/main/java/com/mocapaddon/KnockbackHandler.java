package com.mocapaddon;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Mocap's FakePlayer never runs physics (its tick() skips super) and its position is overwritten by
 * recorded movement, so vanilla knockback (a change of deltaMovement) is lost. We keep our own
 * velocity per playback player, simulate vanilla living-entity physics for it, and keep the resulting
 * displacement as an offset that is re-applied whenever Mocap sets a new path position.
 */
public final class KnockbackHandler {
	/** True while a Mocap-replayed (attacker-less generic) hurt is being processed. */
	public static boolean suppress = false;

	private static final Map<Entity, State> STATES = new WeakHashMap<>();
	private static Entity pending = null;
	private static Vec3 before = Vec3.ZERO;

	private static final class State {
		Vec3 vel = Vec3.ZERO;
		Vec3 base;
		Vec3 offset = Vec3.ZERO;
		boolean grounded;
		State(Entity e) { base = e.position(); grounded = e.onGround(); }
	}

	private KnockbackHandler() {}

	/** Mocap's recorded "Hurt" action is a generic, attacker-less hit; it must not shove the player. */
	public static boolean isReplayHurt(DamageSource s) {
		return s.is(DamageTypes.GENERIC) && s.getEntity() == null && s.getDirectEntity() == null
			&& s.getSourcePosition() == null;
	}

	public static void beforeKnockback(Entity e) {
		State s = STATES.computeIfAbsent(e, State::new);
		before = s.vel;
		pending = e;
		e.setDeltaMovement(s.vel); // vanilla formula compounds on the previous velocity
	}

	public static void afterKnockback(Entity e) {
		if (pending != e) return;
		pending = null;
		State s = STATES.get(e);
		if (s == null) return;
		Vec3 after = e.getDeltaMovement();
		if (!after.equals(before)) {
			s.vel = after;
			s.grounded = false;
		}
		e.setDeltaMovement(Vec3.ZERO); // keep the entity itself motionless, like before
	}

	/** Called by the Mocap mixin right after Mocap has placed the entity on its recorded path. */
	public static void onPathPosition(Entity e) {
		if (e == null || STATES.isEmpty() || !ModConfig.knockback()) return;
		State s = STATES.get(e);
		if (s == null) return;
		s.base = e.position();
		if (s.offset.lengthSqr() == 0.0) return;
		e.move(MoverType.SELF, s.offset);
		e.setDeltaMovement(Vec3.ZERO);
		s.offset = e.position().subtract(s.base);
		s.grounded = false; // let gravity re-settle him
	}

	public static void tick(MinecraftServer server) {
		if (STATES.isEmpty()) return;
		if (!ModConfig.knockback()) { STATES.clear(); return; }
		Iterator<Map.Entry<Entity, State>> it = STATES.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Entity, State> en = it.next();
			Entity e = en.getKey();
			if (e == null || e.isRemoved()) { it.remove(); continue; }
			State s = en.getValue();
			if (s.grounded && s.vel.x == 0.0 && s.vel.z == 0.0 && s.vel.y <= 0.0) continue; // at rest
			try {
				simulate(e, s);
			} catch (Throwable t) {
				MocapAddon.LOGGER.warn("Knockback simulation failed; dropping state for {}", e, t);
				it.remove();
			}
		}
	}

	/** Same order as vanilla LivingEntity.travel: move, then friction/gravity. */
	private static void simulate(Entity e, State s) {
		e.setDeltaMovement(s.vel);
		e.move(MoverType.SELF, s.vel);
		Vec3 v = e.getDeltaMovement(); // collisions already zeroed the blocked axes
		s.grounded = e.onGround();
		float friction = 0.91F;
		if (s.grounded) {
			friction = e.level().getBlockState(e.getOnPos()).getBlock().getFriction() * 0.91F;
		}
		double x = v.x * friction;
		double y = (v.y - 0.08) * 0.98;
		double z = v.z * friction;
		if (Math.abs(x) < 0.003) x = 0.0;
		if (Math.abs(z) < 0.003) z = 0.0;
		if (Math.abs(y) < 0.003) y = 0.0;
		s.vel = new Vec3(x, y, z);
		e.setDeltaMovement(Vec3.ZERO);
		s.offset = e.position().subtract(s.base);
	}

	public static void clear() {
		STATES.clear();
		pending = null;
		suppress = false;
	}
}
