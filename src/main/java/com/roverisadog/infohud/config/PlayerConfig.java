package com.roverisadog.infohud.config;

import com.roverisadog.infohud.InfoHUD;
import com.roverisadog.infohud.Util;
import org.bukkit.ChatColor;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Map containing the InfoHUD configuration options for all player who have it enabled. Does not
 * implement {@link Map} for simplicity. Persistent values are updated in parallel with the
 * plugin's configuration file, but uses a {@link ConcurrentHashMap} for per-update retrievals
 * for better performance.
 */
public class PlayerConfig {

	/** Map containing the configurations for every player. */
	private final Map<UUID, PlayerCfg> playerMap;
	/** Reference to the plugin instance using this PlayerCfgMap. */
	private final InfoHUD pluginInstance;

	public PlayerConfig(InfoHUD pluginInstance) {
		this.pluginInstance = pluginInstance;
		this.playerMap = new ConcurrentHashMap<>();
	}

	/**
	 * Checks whether a player has InfoHUD enabled.
	 * @param player The player.
	 * @return True if enabled.
	 */
	public boolean isEnabled(Player player) {
		return playerMap.containsKey(player.getUniqueId());
	}

	/**
	 * Checks whether a player has InfoHUD enabled.
	 * @param id The player's UUID.
	 * @return True if enabled.
	 */
	public PlayerCfg getConfig(UUID id) {
		return playerMap.get(id);
	}

	/**
	 * Gets the InfoHUD configuration for a player.
	 * @param player The player.
	 * @return The configuration.
	 */
	public PlayerCfg getConfig(Player player) {
		return getConfig(player.getUniqueId());
	}

	/* ---------------------------------- Player Management ---------------------------------- */

	/**
	 * Try to save a player into the map and send said player a chat message signaling success
	 * or failure of the operation.
	 */
	public void addPlayer(Player player) {
		if (isEnabled(player)) {
			Util.sendMsg(player, Util.HLT + "InfoHUD was already enabled.");
		}

		//Initialize player config with default values
		playerMap.put(player.getUniqueId(), new PlayerCfg(player.getUniqueId()));

		// Writes changes into config.yml
		String playerPath = Util.PLAYER_CFG_PATH + "." + player.getUniqueId();
		pluginInstance.getConfig().set(playerPath, getConfig(player).toRawMap());
		pluginInstance.saveConfig();

		Util.sendMsg(player, "InfoHUD is now " + (isEnabled(player)
				? Util.GRN + "enabled" : Util.ERR + "disabled") + Util.RES + ".");
	}

	/**
	 * Disables InfoHUD (remove from map) for a player.
	 *
	 * @param player Player to remove
	 */
	public void removePlayer(Player player) {
		if (!isEnabled(player)) {
			Util.sendMsg(player, Util.HLT + "InfoHUD was already disabled.");
		}

		playerMap.remove(player.getUniqueId());

		// Writes changes into config.yml
		String playerPath = Util.PLAYER_CFG_PATH + "." + player.getUniqueId();
		pluginInstance.getConfig().set(playerPath, null); // null => remove
		pluginInstance.saveConfig();

		Util.sendMsg(player, "InfoHUD is now " + (isEnabled(player)
				? Util.GRN + "enabled" : Util.ERR + "disabled") + Util.RES + ".");
	}

	/* ---------------------------------- Coordinates Mode ---------------------------------- */

	/**
	 * Gets the coordinates display setting for a player.
	 * @param p Concerned player
	 * @return Coordinates display setting
	 */
	public CoordMode getCoordinatesMode(Player p) {
		return playerMap.get(p.getUniqueId()).getCoordMode();
	}

	/**
	 * Changes the coordinates display setting for a player.
	 * @param p Concerned player.
	 * @param newMode New coordinates display setting.
	 * @return
	 */
	public String setCoordinatesMode(Player p, CoordMode newMode) {
		if (!isEnabled(p)) return "";
		playerMap.get(p.getUniqueId()).setCoordMode(newMode);

		// Writes changes into config.yml
		String playerPath = Util.PLAYER_CFG_PATH + "." + p.getUniqueId();
		pluginInstance.getConfig().set(playerPath, getConfig(p).toRawMap());
		pluginInstance.saveConfig();
		return "Coordinates display set to: " + Util.HLT + newMode.description + Util.RES + ".";
	}

	/* ---------------------------------- Time Mode ---------------------------------- */

	/**
	 * Gets the time display format for a player.
	 * @param p Concerned player.
	 * @return Time display format.
	 */
	public TimeMode getTimeMode(Player p) {
		return playerMap.get(p.getUniqueId()).getTimeMode();
	}

	/**
	 * Changes the time display format for a player.
	 * @param p Concerned player.
	 * @param newMode New time display format.
	 * @return
	 */
	public String setTimeMode(Player p, TimeMode newMode) {
		if (newMode == TimeMode.VILLAGER_SCHEDULE &&  Util.apiVersion < 14) {
			return Util.ERR + "Villager schedule display is meaningless for versions before 1.14.";
		}

		playerMap.get(p.getUniqueId()).setTimeMode(newMode);

		// Writes changes into config.yml
		String playerPath = Util.PLAYER_CFG_PATH + "." + p.getUniqueId();
		pluginInstance.getConfig().set(playerPath, getConfig(p).toRawMap());
		pluginInstance.saveConfig();
		return "Time display set to: " + Util.HLT + newMode.description + Util.RES + ".";
	}

	/* ---------------------------------- Dark Mode ---------------------------------- */

	/**
	 * Gets the dark mode setting for a player.
	 * @param p Concerned player.
	 * @return Dark mode setting.
	 */
	public DarkMode getDarkMode(Player p) {
		return playerMap.get(p.getUniqueId()).getDarkMode();
	}

	/**
	 * Changes the dark mode setting for a player.
	 * @param p Concerned player.
	 * @param newMode New dark mode setting.
	 * @return
	 */
	public String setDarkMode(Player p, DarkMode newMode) {
		playerMap.get(p.getUniqueId()).setDarkMode(newMode);

		// Writes changes into config.yml
		String playerPath = Util.PLAYER_CFG_PATH + "." + p.getUniqueId();
		pluginInstance.getConfig().set(playerPath, getConfig(p).toRawMap());
		pluginInstance.saveConfig();
		return "Dark mode set to: " + Util.HLT + newMode.description + Util.RES + ".";
	}

	/**
	 * Updates whether a player is in a bright biome (cached value). Expensive and expected to
	 * be periodically called from the BiomeUpdaterTask.
	 */
	public void updateIsInBrightBiome(Player p) {
		getConfig(p).setInBrightBiome(Util.brightBiomes.contains(p.getLocation().getBlock().getBiome()));
	}

	/* -------------------------------- config.yml management -------------------------------- */

	/**
	 * Loads the contents of config.yml into the player map, upgrading config.yml if it is
	 * outdated (pre-1.2). <b>This method also configures some of the plugin's settings such
	 * as messageUpdateDelay and biomeUpdateDelay.</b>
	 * @return False if any unhandled exception is found.
	 */
	public boolean loadConfig() {
		pluginInstance.reloadConfig();
		FileConfiguration fileConfig = pluginInstance.getConfig();

		// Update config file if applicable.
		if (isOldFileRevision()) {
			updateConfigFile();
		}

		// Load the message update delay [config.yml/"messageUpdateDelay"]
		Util.messageUpdateDelay = fileConfig.getLong(Util.MESSAGE_UPDATE_DELAY_PATH);
		if (Util.messageUpdateDelay == 0L) { // If DNE, getLong() returns 0L.
			Util.messageUpdateDelay = Util.DEFAULT_MESSAGE_UPDATE_DELAY;
		}

		// Load the biome update delay [config.yml/"biomeUpdateDelay"]
		Util.biomeUpdateDelay = fileConfig.getLong(Util.BIOME_UPDATE_DELAY_PATH);
		if (Util.biomeUpdateDelay == 0L) { // If DNE, getLong() returns 0L.
			Util.biomeUpdateDelay = Util.DEFAULT_BIOME_UPDATE_DELAY;
		}

		// Load light and dark mode colours' escape characters [config.yml/"colors.xyz"]
		try {
			Util.bright1 = Util.getColor(fileConfig.getString(Util.COLOR_PATH + ".bright1")).toString();
			Util.bright2 = Util.getColor(fileConfig.getString(Util.COLOR_PATH + ".bright2")).toString();
			Util.dark1   = Util.getColor(fileConfig.getString(Util.COLOR_PATH + ".dark1")).toString();
			Util.dark2   = Util.getColor(fileConfig.getString(Util.COLOR_PATH + ".dark2")).toString();
		} catch (NullPointerException e) {
			e.printStackTrace();
			Util.printToTerminal(Util.ERR + "Error loading one or more colors, using default values.");
			Util.bright1 = Util.GLD;
			Util.bright2 = Util.WHI;
			Util.dark1   = Util.DBLU;
			Util.dark2   = Util.DAQA;
		}

		// Load every players' settings [config.yml/"playerCfg"]
		List<UUID> allPlayers = fileConfig.getConfigurationSection(Util.PLAYER_CFG_PATH)
				.getKeys(false).stream()
				.map(UUID::fromString)
				.collect(Collectors.toList());
		for (UUID playerUUID : allPlayers) {

			//Get raw mapping of the player's settings
			Map<String, Object> rawSettings =
					fileConfig.getConfigurationSection(Util.PLAYER_CFG_PATH + "." + playerUUID.toString())
					.getValues(false);

			// Decode into a PlayerCfg object and put into the player map.
			PlayerCfg playerCfg = PlayerCfg.fromRawMap(playerUUID, rawSettings);
			playerMap.put(playerUUID, playerCfg);
		}

		// Load bright biomes [config.yml/"brightBiomes"]
		Util.brightBiomes = EnumSet.noneOf(Biome.class);
		for (String currentBiome : fileConfig.getStringList(Util.BRIGHT_BIOMES_PATH)) {
			try {
				Biome bio = Biome.valueOf(currentBiome);
				Util.brightBiomes.add(bio);
			}
			catch (IllegalArgumentException ignored) {
				// Biome misspelled, nonexistent, from future or past version.
			}
		}

		return true;
	}

	/**
	 * Checks whether config.yml is from an older version than the current version.
	 * So far, config.yml has only been changed 1.2 -> 1.4.
	 * Things that changed from 1.2 onward:
	 * <ul>
	 *     <li>There is no key <code>infohudVersion</code>.</li>
	 *     <li>There is no key <code>colors</code>.</li>
	 *     <li>There is no key <code>biomeUpdateDelay</code>.</li>
	 *     <li><code>messageUpdateDelay</code> was then named <code>refreshRate</code>.</li>
	 *     <li>
	 *         Each player's <code>coordinatesMode/timeMode/darkMode</code> are stored as integers
	 *         instead of strings.
	 *     </li>
	 * </ul>
	 * So for now, only check for nonexistent keys. This method has to be updated every time
	 * config.yml is changed.
	 * @return True if config.yml is outdated.
	 */
	private boolean isOldFileRevision() {
		FileConfiguration file = pluginInstance.getConfig();
		return file.get("biomeUpdateDelay") == null
				|| file.get("infohudVersion") == null
				|| file.get("colors") == null;
	}

	/** Updates old config file to new format. */
	private void updateConfigFile() {
		Util.printToTerminal(Util.GRN + "Old config file detected: updating...");

		FileConfiguration file = pluginInstance.getConfig();

		/* ================================ Saving Old Data ================================ */

		// Saves old variables into code.
		List<String> oldBrightBiomesList = file.getStringList("brightBiomes");
		Long messageUpdateDelay = file.getLong("refreshRate"); // Now renamed "updateMessageDelay"

		// Save old player configurations
		List<PlayerCfg> playerCfgs = new ArrayList<>();
		for (UUID playerUUID : file.getConfigurationSection("playerCfg").getKeys(false).stream().map(UUID::fromString).collect(Collectors.toList())) {

			//Get raw mapping of the player's settings
			Map<String, Object> rawSettings =
					file.getConfigurationSection("playerCfg." + playerUUID.toString())
							.getValues(false);

			// Decode into a PlayerCfg object and put into a list.
			PlayerCfg playerCfg = PlayerCfg.fromRawMap(playerUUID, rawSettings);
			playerCfgs.add(playerCfg);
		}

		/* ================================ Wiping file ================================ */

		// Set all config data to null, and save (wipe all K-V pairs)
		for (String key : file.getKeys(false)) {
			file.set(key, null);
		}
		pluginInstance.saveConfig();

		/* ================================ Rebuilding file ================================ */

		// Rewrite old variables, add new variables and save.
		file.set(Util.VERSION_PATH, pluginInstance.getDescription().getVersion()); // DNE before
		file.set(Util.MESSAGE_UPDATE_DELAY_PATH, messageUpdateDelay); // messageUpdateDelay: 5
		file.set(Util.BIOME_UPDATE_DELAY_PATH, Util.DEFAULT_BIOME_UPDATE_DELAY); // DNE before

		// Write colours (DNE 1.2 and before)
		file.set(Util.COLOR_PATH + ".bright1", ChatColor.GOLD.name());
		file.set(Util.COLOR_PATH + ".bright2", ChatColor.WHITE.name());
		file.set(Util.COLOR_PATH + ".dark1", ChatColor.DARK_BLUE.name());
		file.set(Util.COLOR_PATH + ".dark2", ChatColor.DARK_AQUA.name());

		// Rewrite bright biomes
		file.set(Util.BRIGHT_BIOMES_PATH, oldBrightBiomesList); //brightBiomes:

		// Rewrite player configs
		for (PlayerCfg playerCfg : playerCfgs) { //playerConfig:
			file.createSection(Util.PLAYER_CFG_PATH + "." + playerCfg.id, playerCfg.toRawMap());
		}

		pluginInstance.saveConfig();

		Util.printToTerminal("Done");
	}

}
