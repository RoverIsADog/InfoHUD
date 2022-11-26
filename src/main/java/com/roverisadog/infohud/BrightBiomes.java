package com.roverisadog.infohud;

import com.roverisadog.infohud.config.DarkMode;
import com.roverisadog.infohud.config.PlayerCfg;
import com.roverisadog.infohud.config.ConfigManager;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;


public class BrightBiomes  {

	/** Key in the config for the biome updater task frequency (long). */
	public static final String BIOME_UPDATE_DELAY_PATH = "biomeUpdateDelay";
	/** key in the config for all bright biomes for all versions (list of string) */
	public static final String BRIGHT_BIOMES_PATH = "brightBiomes";
	/** Default value in case the key-value is missing in the config. */
	public static final int DEFAULT_BIOME_UPDATE_DELAY = 40;
	/** Delay between each biome detection update. Generated on load config. */
	public static long biomeUpdateDelay = 40L;

	/** Biomes from THIS mc version that are considered bright. Generated on load config. */
	public static EnumSet<Biome> brightBiomes;
	/** Default values in case the key-value is missing in the config. */
	static final String[] defaultBrightBiomes = {
			"DESERT", "DESERT_HILLS", "ICE_DESERT", "BEACH", "SNOWY_BEACH",
			"COLD_BEACH", "SNOWY_TUNDRA", "COLD_TUNDRA", "ICE_FLATS", "MUTATED_ICE_FLATS",
			"SNOWY_TAIGA", "SNOWY_TAIGA_HILLS", "SNOWY_TAIGA_MOUNTAINS", "COLD_TAIGA",
			"COLD_TAIGA_HILLS", "COLD_TAIGA_MOUNTAINS", "ICE_MOUNTAINS", "SNOWY_MOUNTAINS",
			"COLD_MOUNTAINS", "EXTREME_HILLS", "EXTREME_HILLS_PLUS_MOUNTAINS", "GRAVELLY_MOUNTAINS",
			"MODIFIED_GRAVELLY_MOUNTAINS", "ICE_PLAINS", "ICE_PLAINS_SPIKE", "ICE_SPIKES", "FROZEN_RIVER",
			/* Caves and cliffs */
			"GROVE", "SNOWY_SLOPES", "JAGGED_PEAKS", "FROZEN_PEAKS", "STONY_PEAKS", "WINDSWEPT_HILLS",
			"WINDSWEPT_GRAVELLY_HILLS", "SNOWY_PLAINS"
	};

	/**
	 * Get a list of all bright biomes currently (in this mc version) considered bright.
	 * Not the same as all bright biomes on the config file as some may not be recognised
	 * by older/newer minecraft versions.
	 * @return List of bright biomes' names.
	 */
	protected static List<String> getBrightBiomesList() {
		return brightBiomes.stream()
				.map(Biome::toString)
				.collect(Collectors.toList());
	}

	/**
	 * Resets the current bright biomes list to the default values and flush it to file.
	 * @return True if successfully reset, false otherwise.
	 */
	protected static boolean resetBrightBiomes() {
		try {
			InfoHUD.getPlugin().getConfig().set(BRIGHT_BIOMES_PATH, defaultBrightBiomes);
			InfoHUD.getPlugin().saveConfig();
			brightBiomes.clear();
			for (String b : defaultBrightBiomes) {
				try {
					Biome bio = Biome.valueOf(b);
					brightBiomes.add(bio);
				} catch (Exception ignored) {} // DNE in current version
			}
			return true;

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/** @return How many ticks between each biome change check. */
	protected static long getBiomeUpdateDelay() {
		return biomeUpdateDelay;
	}

	/**
	 * Runnable responsible for getting each online player's biome and updating their config
	 * accordingly (cached value "isInBrightBiome"). This task is comparatively very expensive
	 * and should be run at larger intervals.
	 */
	static class BrightBiomesUpdaterTask implements Runnable {

		/** Instance of the InfoHUD plugin. */
		private final InfoHUD pluginInstance;
		/** Object containing the configurations of all players. */
		private final ConfigManager configManager;

		public BrightBiomesUpdaterTask(InfoHUD pluginInstance) {
			this.pluginInstance = pluginInstance;
			this.configManager = pluginInstance.getConfigManager();
		}

		@Override
		public void run() {
			//getOnlinePlayers() is not thread safe. Can't be accessed asynchronously :(
			for (Player p : pluginInstance.getServer().getOnlinePlayers()) {
				if (configManager.updateAndGetEnabled(p)) {
					PlayerCfg cfg = configManager.getCfg(p);
					if (cfg.getDarkMode() == DarkMode.AUTO) {
						cfg.setInBrightBiome(brightBiomes.contains(p.getLocation().getBlock().getBiome()));
					}
				}
			}
		}

	}
}
