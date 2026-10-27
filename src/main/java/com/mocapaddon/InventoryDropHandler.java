package com.mocapaddon;

import net.minecraft.world.entity.LivingEntity;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Guarantees an inventory is dropped at most once per death (no duplication). */
public final class InventoryDropHandler {
	private static final Set<LivingEntity> DROPPED = Collections.newSetFromMap(new WeakHashMap<>());

	private InventoryDropHandler() {}

	/** @return true if this is the first drop for the current death */
	public static boolean markDropped(LivingEntity e) { return DROPPED.add(e); }

	/** Called when the entity is seen alive again (e.g. Mocap respawn). */
	public static void reset(LivingEntity e) { if (!DROPPED.isEmpty()) DROPPED.remove(e); }

	public static void clear() { DROPPED.clear(); }
}
