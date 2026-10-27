package com.mocapaddon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** config/mocap_addon.json: {"inventorydrop": true, "knockback": true} */
public final class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static volatile boolean inventoryDrop = true;
	private static volatile boolean knockback = true;

	private ModConfig() {}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("mocap_addon.json");
	}

	public static boolean inventoryDrop() { return inventoryDrop; }
	public static boolean knockback() { return knockback; }

	public static void setInventoryDrop(boolean v) { inventoryDrop = v; save(); }
	public static void setKnockback(boolean v) { knockback = v; save(); }

	public static synchronized void load() {
		Path p = path();
		if (!Files.exists(p)) { save(); return; }
		try (Reader r = Files.newBufferedReader(p)) {
			JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
			if (o.has("inventorydrop")) inventoryDrop = o.get("inventorydrop").getAsBoolean();
			if (o.has("knockback")) knockback = o.get("knockback").getAsBoolean();
		} catch (Exception e) {
			MocapAddon.LOGGER.warn("Could not read {}, using defaults (true/true).", p, e);
		}
	}

	public static synchronized void save() {
		Path p = path();
		try {
			Files.createDirectories(p.getParent());
			JsonObject o = new JsonObject();
			o.addProperty("inventorydrop", inventoryDrop);
			o.addProperty("knockback", knockback);
			Path tmp = p.resolveSibling("mocap_addon.json.tmp");
			try (Writer w = Files.newBufferedWriter(tmp)) { GSON.toJson(o, w); }
			try {
				Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (IOException e) {
				Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (Exception e) {
			MocapAddon.LOGGER.warn("Could not save {}", p, e);
		}
	}
}
