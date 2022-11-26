package com.roverisadog.infohud.config;

import com.roverisadog.infohud.BrightBiomes;
import com.roverisadog.infohud.InfoHUD;
import com.roverisadog.infohud.MessageUpdaterTask;
import com.roverisadog.infohud.Util;
import org.bukkit.ChatColor;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Object responsible for managing all settings. Plugin settings are saved in {@code config.yml}
 * and are accessed using {@link Plugin#getConfig()} as usual. Per-player settings are saved in
 * {@code players.yml} from 1.5 onward and can be accessed using {@link #getPlayersConfig()}.
 * <br>
 * This class also contains a Map for faster per-player settings access.
 * <br>
 * If possible, all configuration changes should go through this class for file write concurrency
 * control and so that the config and internal maps are updated at the same time.
 */
public class ConfigManager {

	/** Key in config.yml for the infohud version that created the file. */
	protected static final String VERSION_PATH = "infohudVersion";
	/** Key in config.yml for the colour configurations. */
	protected static final String COLOR_PATH = "colors";
	/** Key in players.yml for the list of player configurations (list of map) */
	protected static final String PLAYER_CFG_PATH = "playerConfig";

	/** File containing per-player settings (moved InfoHUD 1.5). */
	private File playerConfigFile;
	/** Name of the file containing per-player settings (moved InfoHUD 1.5). */
	protected static final String PLAYER_CFG_FILENAME = "players.yml";
	/** Configuration object for per-player settings, backed by {@code players.yml}. */
	private FileConfiguration playersConfig;
	/** Map containing the configurations for every player. */
	private final Map<UUID, PlayerCfg> playerMap;
	/** Reference to the plugin instance using this PlayerCfgMap. */
	private final InfoHUD pluginInstance;

	public ConfigManager(InfoHUD pluginInstance) {
		this.pluginInstance = pluginInstance;
		this.playerMap = new ConcurrentHashMap<>();
	}

	/**
	 * Updates the player's config (e.g. update the pause timer) and then checks whether the player
	 * has InfoHUD enabled for this tick.
	 * @param player The player.
	 * @return True if enabled for this tick.
	 */
	public boolean updateAndGetEnabled(Player player) {
		PlayerCfg cfg = playerMap.get(player.getUniqueId());
		if (cfg == null) return false;
		return cfg.updatePaused();
	}

	/**
	 * Gets the InfoHUD configuration object for a player.
	 * @param id The player's UUID.
	 * @return The configuration.
	 */
	public PlayerCfg getCfg(UUID id) {
		return playerMap.get(id);
	}

	/**
	 * Gets the InfoHUD configuration object for a player.
	 * @param player The player.
	 * @return The configuration.
	 */
	public PlayerCfg getCfg(Player player) {
		return getCfg(player.getUniqueId());
	}

	/* ---------------------------------- Player Management ---------------------------------- */

	/**
	 * Try to save a player into the map and send said player a chat message signaling success
	 * or failure of the operation and writes changes to file.
	 */
	public synchronized void addPlayer(Player player) {
		if (updateAndGetEnabled(player)) {
			Util.sendMsg(player, Util.HLT + "InfoHUD was already enabled.");
			return;
		}

		//Initialize player config with default values
		playerMap.put(player.getUniqueId(), new PlayerCfg(player.getUniqueId()));

		// Writes changes into config.yml
		String playerPath = PLAYER_CFG_PATH + "." + player.getUniqueId();
		playersConfig.set(playerPath, getCfg(player).toRawMap());
		savePlayersConfig();

		Util.sendMsg(player, "InfoHUD is now " + (updateAndGetEnabled(player)
				? Util.GRN + "enabled" : Util.ERR + "disabled") + Util.RES + ".");
	}

	/**
	 * Disables InfoHUD (remove from map) for a player and writes changes to file.
	 *
	 * @param player Player to remove
	 */
	public synchronized void removePlayer(Player player) {
		if (!updateAndGetEnabled(player)) {
			Util.sendMsg(player, Util.HLT + "InfoHUD was already disabled.");
			return;
		}

		playerMap.remove(player.getUniqueId());

		// Writes changes into config.yml
		String playerPath = PLAYER_CFG_PATH + "." + player.getUniqueId();
		playersConfig.set(playerPath, null); // null => remove
		savePlayersConfig();

		Util.sendMsg(player, "InfoHUD is now " + (updateAndGetEnabled(player)
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
	 * Changes the coordinates display setting for a player and writes changes to file.
	 * @param p Concerned player.
	 * @param newMode New coordinates display setting.
	 * @return True if successful.
	 */
	public synchronized boolean setCoordinatesMode(Player p, CoordMode newMode) {
		if (!updateAndGetEnabled(p)) return false;
		playerMap.get(p.getUniqueId()).setCoordMode(newMode);

		// Writes changes into config.yml
		String playerPath = PLAYER_CFG_PATH + "." + p.getUniqueId();
		playersConfig.set(playerPath, getCfg(p).toRawMap());
		savePlayersConfig();
		return true;
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
	 * Changes the time display format for a player and writes changes to file.
	 * @param p Concerned player.
	 * @param newMode New time display format.
	 * @return True if successful.
	 */
	public synchronized boolean setTimeMode(Player p, TimeMode newMode) {
		playerMap.get(p.getUniqueId()).setTimeMode(newMode);

		// Writes changes into config.yml
		String playerPath = PLAYER_CFG_PATH + "." + p.getUniqueId();
		playersConfig.set(playerPath, getCfg(p).toRawMap());
		savePlayersConfig();
		return true;
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
	 * Changes the dark mode setting for a player and writes changes to file.
	 * @param p Concerned player.
	 * @param newMode New dark mode setting.
	 * @return True if successful.
	 */
	public synchronized boolean setDarkMode(Player p, DarkMode newMode) {
		playerMap.get(p.getUniqueId()).setDarkMode(newMode);

		// Writes changes into config.yml
		String playerPath = PLAYER_CFG_PATH + "." + p.getUniqueId();
		playersConfig.set(playerPath, getCfg(p).toRawMap());
		savePlayersConfig();
		return true;
	}

	/* ---------------------------------- Pause Settings ---------------------------------- */

	/**
	 * Pauses message display for a given player.
	 * @param p The player.
	 * @param time Ticks to stop for.
	 */
	public void pauseFor(Player p, int time) {
		playerMap.get(p.getUniqueId()).pauseFor(time);
	}

	/**
	 * Pauses message display for all players in the config.
	 * @param time Ticks to stop for.
	 */
	public synchronized void pauseFor(int time) {
		playerMap.forEach((uuid, playerCfg) -> playerCfg.pauseFor(time));
	}



	/* -------------------------------- File management -------------------------------- */

	/**
	 * Loads the contents of config.yml and players.yml into the player map and other variables,
	 * upgrading config.yml if it is outdated. <b>This method also configures some of the plugin's
	 * settings such as messageUpdateDelay and biomeUpdateDelay.</b>
	 * @return False if any unhandled exception is encountered.
	 */
	public synchronized boolean loadConfig() {
		// Get the configuration objects
		pluginInstance.reloadConfig();
		FileConfiguration pluginConfig = pluginInstance.getConfig();
		createPlayersConfig();

		// Update config file if applicable.
		if (isOldFileRevision()) {
			updateConfigFile();
		}

		// Load the message update delay [config.yml/"messageUpdateDelay"]
		MessageUpdaterTask.messageUpdateDelay = pluginConfig.getLong(MessageUpdaterTask.MESSAGE_UPDATE_DELAY_PATH);
		if (MessageUpdaterTask.messageUpdateDelay == 0L) { // If DNE, getLong() returns 0L.
			MessageUpdaterTask.messageUpdateDelay = MessageUpdaterTask.DEFAULT_MESSAGE_UPDATE_DELAY;
		}

		// Load the biome update delay [config.yml/"biomeUpdateDelay"]
		BrightBiomes.biomeUpdateDelay = pluginConfig.getLong(BrightBiomes.BIOME_UPDATE_DELAY_PATH);
		if (BrightBiomes.biomeUpdateDelay == 0L) { // If DNE, getLong() returns 0L.
			BrightBiomes.biomeUpdateDelay = BrightBiomes.DEFAULT_BIOME_UPDATE_DELAY;
		}

		// Load light and dark mode colours' escape characters [config.yml/"colors.xyz"]
		try {
			MessageUpdaterTask.bright1 = Util.getColor(pluginConfig.getString(COLOR_PATH + ".bright1")).toString();
			MessageUpdaterTask.bright2 = Util.getColor(pluginConfig.getString(COLOR_PATH + ".bright2")).toString();
			MessageUpdaterTask.dark1   = Util.getColor(pluginConfig.getString(COLOR_PATH + ".dark1")).toString();
			MessageUpdaterTask.dark2   = Util.getColor(pluginConfig.getString(COLOR_PATH + ".dark2")).toString();
		} catch (NullPointerException e) {
			e.printStackTrace();
			Util.printToTerminal(Util.ERR + "Error loading one or more colors, using default values.");
			MessageUpdaterTask.bright1 = Util.GLD;
			MessageUpdaterTask.bright2 = Util.WHI;
			MessageUpdaterTask.dark1   = Util.DBLU;
			MessageUpdaterTask.dark2   = Util.DAQA;
		}

		// Load bright biomes [config.yml/"brightBiomes"]
		BrightBiomes.brightBiomes = EnumSet.noneOf(Biome.class);
		for (String currentBiome : pluginConfig.getStringList(BrightBiomes.BRIGHT_BIOMES_PATH)) {
			try {
				Biome bio = Biome.valueOf(currentBiome);
				BrightBiomes.brightBiomes.add(bio);
			}
			catch (IllegalArgumentException ignored) {
				// Biome misspelled, nonexistent, from future or past version.
			}
		}

		// Load every players' settings [players.yml/"playerCfg"]
		List<UUID> allPlayers = playersConfig.getConfigurationSection(PLAYER_CFG_PATH)
				.getKeys(false).stream()
				.map(UUID::fromString)
				.collect(Collectors.toList());
		for (UUID playerUUID : allPlayers) {

			//Get raw mapping of the player's settings
			Map<String, Object> rawSettings =
					playersConfig.getConfigurationSection(PLAYER_CFG_PATH + "." + playerUUID.toString())
					.getValues(false);

			// Decode into a PlayerCfg object and put into the player map.
			PlayerCfg playerCfg = PlayerCfg.fromRawMap(playerUUID, rawSettings);
			playerMap.put(playerUUID, playerCfg);
		}
		return true;
	}

	/**
	 * Creates the player configuration {@link File} if it did not exist and initialise
	 * the {@link FileConfiguration} object. Similar in functionality to
	 * {@link Plugin#reloadConfig()}
	 */
	private void createPlayersConfig() {
		// Create file if DNE
		playerConfigFile = new File(pluginInstance.getDataFolder(), PLAYER_CFG_FILENAME);
		if (!playerConfigFile.exists()) {
			boolean ignored = playerConfigFile.getParentFile().mkdirs();
			pluginInstance.saveResource(PLAYER_CFG_FILENAME, false);
		}
		playersConfig = YamlConfiguration.loadConfiguration(playerConfigFile);
	}

	/**
	 * Gets the {@link FileConfiguration} backed by {@code players.yml}. Equivalent to
	 * {@link Plugin#getConfig()} for player settings.
	 * @return The file configuration.
	 */
	public FileConfiguration getPlayersConfig() {
		return playersConfig;
	}

	/**
	 * Saves player settings in the {@link FileConfiguration} into {@code players.yml}.
	 * Equivalent to {@link Plugin#saveConfig()} for player settings.
	 */
	public synchronized void savePlayersConfig() {
		try {
			playersConfig.save(playerConfigFile);
		} catch (IOException e) {
			Util.printToTerminal("Error while saving player configuration file.");
			e.printStackTrace();
		}
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
	 *     <li><code>playerConfig</code> was then named <code>playerCfg</code>.</li>
	 *     <li>
	 *         Each player's <code>coordinatesMode/timeMode/darkMode</code> are stored as integers
	 *         instead of strings.
	 *     </li>
	 * </ul>
	 * Things that changed from 1.5 onward:
	 * <ul>
	 *     <li>
	 *         Move player configuration save location from <code>config.yml</code> to
	 *         <code>players.yml</code> in the same folder (key and format the same).
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
				|| file.get("colors") == null
				// Players on different file.
				|| file.get("playerConfig") != null;
	}

	/**
	 * Updates old any old (valid) config file to the latest format by saving all the data
	 * contained and rewriting the files entirely.
	 */
	private synchronized void updateConfigFile() {
		Util.printToTerminal(Util.GRN + "Old config file detected: updating...");

		FileConfiguration config = pluginInstance.getConfig();

		/* ================================ Saving Old Data ================================ */
		Util.printToTerminal("Saving old data");

		// Saves old variables into code.
		List<String> oldBrightBiomesList = config.getStringList("brightBiomes");
		Long messageUpdateDelay = config.getLong("refreshRate"); // Now renamed "updateMessageDelay"

		// Save old player configurations (Integers instead of strings), playerCfg -> playerConfig
		List<PlayerCfg> playerCfgs = new ArrayList<>();
		if (config.isConfigurationSection("playerCfg")) {
			for (String playerUUIDStr : config.getConfigurationSection("playerCfg").getKeys(false)) {

				//Get raw mapping of the player's settings
				Map<String, Object> rawSettings = config
						.getConfigurationSection("playerCfg." + playerUUIDStr)
						.getValues(false);

				// Decode into a PlayerCfg object and put into a list.
				PlayerCfg playerCfg = PlayerCfg.fromRawMap(UUID.fromString(playerUUIDStr), rawSettings);
				playerCfgs.add(playerCfg);
			}
		}
		// New player save format, but on config.yml instead of players.yml. "playerConfig"
		else if (config.isConfigurationSection("playerConfig")) {

			for (String playerUUIDStr : config.getConfigurationSection("playerConfig").getKeys(false)) {
				//Get raw mapping of the player's settings
				Map<String, Object> rawSettings = config
						.getConfigurationSection("playerConfig." + playerUUIDStr)
						.getValues(false);

				// Decode into a PlayerCfg object and put into a list.
				PlayerCfg playerCfg = PlayerCfg.fromRawMap(UUID.fromString(playerUUIDStr), rawSettings);
				playerCfgs.add(playerCfg);
			}
		}

		/* ================================ Wiping files ================================ */
		Util.printToTerminal("Wiping files");

		// Set all config data to null, and save (wipe all K-V pairs)
		for (String key : config.getKeys(false)) {
			config.set(key, null);
		}
		pluginInstance.saveConfig();
		for (String key : playersConfig.getKeys(false)) {
			playersConfig.set(key, null);
		}
		savePlayersConfig();

		/* ================================ Rebuilding file ================================ */
		Util.printToTerminal("Rebuilding files");

		// Rewrite old variables, add new variables and save.
		config.set(VERSION_PATH, pluginInstance.getDescription().getVersion()); // DNE before
		config.set(MessageUpdaterTask.MESSAGE_UPDATE_DELAY_PATH, messageUpdateDelay); // messageUpdateDelay: 5
		config.set(BrightBiomes.BIOME_UPDATE_DELAY_PATH, BrightBiomes.DEFAULT_BIOME_UPDATE_DELAY); // DNE before

		// Write colours (DNE 1.2 and before)
		config.set(COLOR_PATH + ".bright1", ChatColor.GOLD.name());
		config.set(COLOR_PATH + ".bright2", ChatColor.WHITE.name());
		config.set(COLOR_PATH + ".dark1", ChatColor.DARK_BLUE.name());
		config.set(COLOR_PATH + ".dark2", ChatColor.DARK_AQUA.name());

		// Rewrite bright biomes
		config.set(BrightBiomes.BRIGHT_BIOMES_PATH, oldBrightBiomesList); //brightBiomes:

		// Rewrite player configs
		playersConfig.set(VERSION_PATH, pluginInstance.getDescription().getVersion());
		for (PlayerCfg playerCfg : playerCfgs) { //playerConfig:
			playersConfig.createSection(PLAYER_CFG_PATH + "." + playerCfg.getId(), playerCfg.toRawMap());
		}

		pluginInstance.saveConfig();
		savePlayersConfig();

		Util.printToTerminal("Done");
	}


}
