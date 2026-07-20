package eu.stefanbraun612.smartautoattack.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

// Separate from Cloth Config's own JSON file since presets are named snapshots of a
// subset of the config, not the live config itself.
public class PresetManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("smartautoattack_presets.json");

	private static Map<String, AttackPreset> cache;

	public static synchronized Map<String, AttackPreset> all() {
		if (cache == null) {
			cache = load();
		}
		return cache;
	}

	public static synchronized void save(String name, AttackPreset preset) {
		all().put(name, preset);
		persist();
	}

	public static synchronized boolean delete(String name) {
		boolean removed = all().remove(name) != null;
		if (removed) {
			persist();
		}
		return removed;
	}

	public static AttackPreset get(String name) {
		return all().get(name);
	}

	private static Map<String, AttackPreset> load() {
		Map<String, AttackPreset> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		if (Files.isRegularFile(FILE)) {
			try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
				Map<String, AttackPreset> loaded = GSON.fromJson(reader, new TypeToken<LinkedHashMap<String, AttackPreset>>() {}.getType());
				if (loaded != null) {
					result.putAll(loaded);
				}
			} catch (IOException | RuntimeException ignored) {
				// Corrupt/unreadable presets file - fall back to just the built-in preset below
				// rather than crashing the mod over a non-essential feature.
			}
		}
		result.putIfAbsent("Creaking_TP_AEHP", AttackPreset.creakingTpAehp());
		return result;
	}

	private static void persist() {
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
				GSON.toJson(all(), writer);
			}
		} catch (IOException ignored) {
			// Non-essential feature - a failed save just means the preset isn't kept for next time.
		}
	}
}
