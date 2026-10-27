package com.mocapaddon;

import java.lang.reflect.Field;

/** All knowledge about Mocap's internals lives here. Nothing links against Mocap at compile time. */
public final class MocapCompat {
	public static final String FAKE_PLAYER = "net.mt1006.mocap.utils.FakePlayer";
	public static final String ACTION_CONTEXT = "net.mt1006.mocap.mocap.playing.playback.ActionContext";

	private static final ClassValue<Boolean> IS_FAKE = new ClassValue<>() {
		@Override
		protected Boolean computeValue(Class<?> type) {
			for (Class<?> c = type; c != null; c = c.getSuperclass()) {
				if (c.getName().equals(FAKE_PLAYER)) return Boolean.TRUE;
			}
			return Boolean.FALSE;
		}
	};

	private static volatile Field killedByPlaybackField;
	private static volatile boolean killedFieldMissing = false;

	private MocapCompat() {}

	public static boolean isFakePlayer(Object o) {
		return o != null && IS_FAKE.get(o.getClass());
	}

	/** True when Mocap itself is "killing" the player (end of recording, cleanup) - not a real kill. */
	public static boolean wasKilledByPlayback(Object fakePlayer) {
		if (killedFieldMissing) return true; // unknown structure: stay safe, do not drop
		try {
			Field f = killedByPlaybackField;
			if (f == null) {
				Class<?> c = fakePlayer.getClass();
				while (c != null && !c.getName().equals(FAKE_PLAYER)) c = c.getSuperclass();
				f = c.getDeclaredField("killedByPlayback");
				f.setAccessible(true);
				killedByPlaybackField = f;
			}
			return f.getBoolean(fakePlayer);
		} catch (Throwable t) {
			killedFieldMissing = true;
			MocapAddon.LOGGER.warn("Mocap's FakePlayer.killedByPlayback not found; inventory drop disabled.", t);
			return true;
		}
	}

	public static void verify() {
		ClassLoader cl = MocapAddon.class.getClassLoader();
		try {
			Class<?> fp = Class.forName(FAKE_PLAYER, false, cl);
			fp.getDeclaredField("killedByPlayback");
		} catch (Throwable t) {
			killedFieldMissing = true;
			MocapAddon.LOGGER.warn("Mocap's FakePlayer class/structure was not found ({}). Inventory drop is disabled; this Mocap version may be unsupported.", t.toString());
		}
		try {
			Class<?> ac = Class.forName(ACTION_CONTEXT, false, cl);
			boolean found = false;
			for (var m : ac.getDeclaredMethods()) if (m.getName().equals("changePosition")) found = true;
			if (!found) throw new NoSuchMethodException("changePosition");
		} catch (Throwable t) {
			MocapAddon.LOGGER.warn("Mocap's ActionContext.changePosition was not found ({}). Knockback displacement may not follow Mocap paths correctly.", t.toString());
		}
	}
}
