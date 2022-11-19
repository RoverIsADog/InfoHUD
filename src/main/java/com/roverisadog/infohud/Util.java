
package com.roverisadog.infohud;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.roverisadog.infohud.command.CoordMode;
import com.roverisadog.infohud.command.DarkMode;
import com.roverisadog.infohud.command.TimeMode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/** Helper class. */
public class Util {

	private Util() {
		throw new IllegalArgumentException();
	}

	//Shortcuts
	public static final String PERM_USE = "infohud.use";
	public static final String PERM_ADMIN = "infohud.admin";

	public static final String HLT = ChatColor.YELLOW.toString();
	public static final String SIGN = ChatColor.DARK_AQUA.toString();
	public static final String ERR = ChatColor.RED.toString();

	public static final String RES = ChatColor.RESET.toString();
	public static final String WHI = ChatColor.WHITE.toString();
	public static final String GLD = ChatColor.GOLD.toString();
	public static final String DAQA = ChatColor.DARK_AQUA.toString();
	public static final String DBLU = ChatColor.DARK_BLUE.toString();
	public static final String GRN = ChatColor.GREEN.toString();

	//Colors
	public static String bright1 = Util.GLD;
	public static String bright2 = Util.WHI;
	public static String dark1 = Util.DBLU;
	public static String dark2 = Util.DAQA;

	/** Minecraft release 1.XX. */
	public static int apiVersion;
	/** CraftBukkit, Spigot, Paper, ... */
	public static String serverVendor;

	/** Currently loaded biomes considered bright. */
	private static EnumSet<Biome> brightBiomes;
	private static final String[] defaultBrightBiomes = {
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

	//Default values
	static final String CMD_NAME = "infohud";

	public static final String BRIGHT_BIOMES_PATH = "brightBiomes";
	public static final String PLAYER_CFG_PATH = "playerConfig";
	public static final String MESSAGE_UPDATE_DELAY_PATH = "messageUpdateDelay";
	public static final String BIOME_UPDATE_DELAY_PATH = "biomeUpdateDelay";
	public static final String VERSION_PATH = "infohudVersion";
	public static final String COLOR_PATH = "colors";

	public static final String SIGNATURE = Util.SIGN + "[InfoHUD] " + Util.RES;

	public static final int DEFAULT_MESSAGE_UPDATE_DELAY = 5;
	public static final int DEFAULT_BIOME_UPDATE_DELAY = 40;


	//Performance
	/** Delay between each actionbar message update. */
	private static long messageUpdateDelay;
	/** Delay between each biome detection update. */
	private static long biomeUpdateDelay = 40L;

	static long benchmark = 0;

	static boolean isFromOlderVersion = false;

	/** Loads disk contents of config.yml into memory. Returns false if an unhandled exception is found. */
	static boolean loadConfig() {
		try {

			InfoHUD.getPlugin().reloadConfig();

			FileConfiguration file = InfoHUD.getPlugin().getConfig();

			//Get the message update delay.
			messageUpdateDelay = file.getLong(MESSAGE_UPDATE_DELAY_PATH);
			//Older versions
			if (messageUpdateDelay == 0L) {
				messageUpdateDelay = DEFAULT_MESSAGE_UPDATE_DELAY;
			}

			//Get the biome update delay.
			biomeUpdateDelay = file.getLong(BIOME_UPDATE_DELAY_PATH);
			if (biomeUpdateDelay == 0L) {
				biomeUpdateDelay = DEFAULT_BIOME_UPDATE_DELAY; //Default value.
			}

			//Get colors
			try {
				bright1 = getColor(file.getString(COLOR_PATH + ".bright1")).toString();
				bright2 = getColor(file.getString(COLOR_PATH + ".bright2")).toString();
				dark1 = getColor(file.getString(COLOR_PATH + ".dark1")).toString();
				dark2 = getColor(file.getString(COLOR_PATH + ".dark2")).toString();
			} catch (Exception e) {
				printToTerminal(ERR + "Error loading one or more colors, using default values.");
				bright1 = Util.GLD;
				bright2 = Util.WHI;
				dark1 = Util.DBLU;
				dark2 = Util.DAQA;
			}

			//Building player settings hash
			PlayerCfg.playerMap = new ConcurrentHashMap<>(); //Map<UUID , PlayerConfig>

			//For every section of "playerCfg" : UUID in string form
			for (String playerStr : file.getConfigurationSection(PLAYER_CFG_PATH).getKeys(false)) {

				//Get UUID from String
				UUID playerID = UUID.fromString(playerStr);

				//Get raw mapping of the player's settings
				Map<String, Object> playerSettings =
						file.getConfigurationSection(PLAYER_CFG_PATH + "." + playerStr).getValues(false);

				//Decode into enumerated types.
				PlayerCfg playerCfg = loadPlayerSettings(playerID, playerSettings);

				//Translate into faster mappings
				PlayerCfg.playerMap.put(playerID, playerCfg);
			}

			//Loading biomes
			brightBiomes = EnumSet.noneOf(Biome.class);
			for (String currentBiome : file.getStringList(BRIGHT_BIOMES_PATH)) {
				try {
					Biome bio = Biome.valueOf(currentBiome);
					brightBiomes.add(bio);
				}
				catch (Exception ignored) {
					//Biome misspelled, nonexistent, from future or past version.
				}
			}

			if (isFromOlderVersion) {
				updateConfigFile();
				isFromOlderVersion = false;
			}

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Loads a specific player's configuration from a Map.
	 * @param map Map to read from.
	 * @return PlayerCfg containing the desired values.
	 */
	static PlayerCfg loadPlayerSettings(UUID id, Map<String, Object> map) {

		CoordMode coordMode;
		TimeMode timeMode;
		DarkMode darkMode;

		//Is from a version before InfoHUD 1.2 (Stored as int)
		if (map.get(CoordMode.cfgKey) instanceof Integer) {
			isFromOlderVersion = true;
			coordMode = CoordMode.get((int) map.get(CoordMode.cfgKey));
			timeMode = TimeMode.get((int) map.get(TimeMode.cfgKey));
			darkMode = DarkMode.get((int) map.get(DarkMode.cfgKey));
		}
		//Is from newer versions (stored as String)
		else {
			coordMode = CoordMode.get((String) map.get(CoordMode.cfgKey));
			timeMode = TimeMode.get((String) map.get(TimeMode.cfgKey));
			darkMode = DarkMode.get((String) map.get(DarkMode.cfgKey));
		}

		return new PlayerCfg(id, coordMode, timeMode, darkMode);
	}

	/** Updates old config file to new format. */
	static void updateConfigFile() {
		String msg = GRN + "Old config file detected: updating...";

		FileConfiguration file = InfoHUD.getPlugin().getConfig();

		//Saves old data into code.
		List<String> oldBiomeList = file.getStringList(BRIGHT_BIOMES_PATH);
		messageUpdateDelay = file.getLong("refreshRate"); //Old name

		//Set all config data to null, and save (wipe)
		for (String key : file.getKeys(false)) {
			InfoHUD.getPlugin().getConfig().set(key, null);
		}
		InfoHUD.getPlugin().saveConfig();

		//Rewrite old data and save.
		file.set(VERSION_PATH, InfoHUD.getPlugin().getDescription().getVersion()); //infohudVersion: '1.3'
		file.set(MESSAGE_UPDATE_DELAY_PATH, messageUpdateDelay); //messageUpdateDelay: 5
		file.set(BIOME_UPDATE_DELAY_PATH, DEFAULT_BIOME_UPDATE_DELAY); //biomeUpdateDelay: 40

		file.set(COLOR_PATH + ".bright1", ChatColor.GOLD.name());
		file.set(COLOR_PATH + ".bright2", ChatColor.WHITE.name());
		file.set(COLOR_PATH + ".dark1", ChatColor.DARK_BLUE.name());
		file.set(COLOR_PATH + ".dark2", ChatColor.DARK_AQUA.name());

		file.set(BRIGHT_BIOMES_PATH, oldBiomeList); //brightBiomes:
		for (UUID id : PlayerCfg.playerMap.keySet()) { //playerConfig:
			file.createSection(PLAYER_CFG_PATH + "." + id.toString(),
					PlayerCfg.playerMap.get(id).toMap());
		}
		InfoHUD.getPlugin().saveConfig();

		msg += " Done";
		printToTerminal(msg);
	}

	/**
	 * Attempts to get a color from a given name.
	 * @param name Name to check.
	 * @return Matching color.
	 * @throws Exception If no matching color is found.
	 * @see <a href="https://minecraft.gamepedia.com/Formatting_codes">Color codes (ALLCAPS)</a>
	 */
	private static ChatColor getColor(String name) throws Exception {
		for (ChatColor col : ChatColor.values()) {
			if (col.name().equalsIgnoreCase(name)) {
				return col;
			}
		}
		throw new Exception();
	}

	/* ---------------------------------------------------------------------- Dark Mode ---------------------------------------------------------------------- */

	/** Returns whether the player is in a bright biome for darkmode. */
	static void updateIsInBrightBiome(Player p) {
		PlayerCfg.playerMap.get(p.getUniqueId()).isInBrightBiome
				= brightBiomes.contains(p.getLocation().getBlock().getBiome());
	}

	/**
	 * Get a list of all bright biomes currently recognized. Not the same as
	 * all bright biomes on the config file as some may not be recognized by
	 * older/newer minecraft versions.
	 */
	static List<String> getBrightBiomesList() {
		return brightBiomes.stream()
				.map(Biome::toString)
				.collect(Collectors.toList());
	}

	/**
	 * Adds a biome from the bright biomes list.
	 * @return Small message confirming status.
	 */
	static String addBrightBiome(Biome b) {
		try {
			//Already there
			if (brightBiomes.contains(b)) {
				return HLT + b.toString() + RES + " is already included in the bright biomes list.";
			}
			else {
				//Add to set
				brightBiomes.add(b);
				//Update config.yml. APPEND MODE to not lose biomes from other versions.
				List<String> biomeList =
						new LinkedList<>(InfoHUD.getPlugin().getConfig().getStringList(BRIGHT_BIOMES_PATH));
				biomeList.add(b.toString());
				InfoHUD.getPlugin().getConfig().set(BRIGHT_BIOMES_PATH, biomeList);
				InfoHUD.getPlugin().saveConfig();
				return GRN + "Added " + HLT + b + GRN + " to the bright biomes list.";
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ERR + "Error while adding " + HLT + b.toString() + ERR + " to bright biomes list.";
		}
	}

	/**
	 * Removes a biome from the bright biomes list.
	 * @return Small message confirming status.
	 */
	static String removeBrightBiome(Biome b) {
		try {
			//Contained
			if (brightBiomes.contains(b)) {
				//Update config.yml. APPEND MODE to not lose biomes from other versions.
				List<String> biomeList =
						new LinkedList<>(InfoHUD.getPlugin().getConfig().getStringList(BRIGHT_BIOMES_PATH));
				biomeList.remove(b.toString());
				InfoHUD.getPlugin().getConfig().set(BRIGHT_BIOMES_PATH, biomeList);
				InfoHUD.getPlugin().saveConfig();

				//Remove from set
				brightBiomes.remove(b);
				return GRN + "Removed " + HLT + b + GRN + " from the bright biomes list.";
			}
			//Wasn't included.
			else {
				return HLT + b.toString() + RES + " isn't in the bright biomes list.";
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ERR + "Error while removing " + HLT + b.toString() + ERR + " from the bright biomes list.";
		}
	}

	static String resetBrightBiomes() {
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
			return GRN + "Reset the bright biomes list.";

		} catch (Exception e) {
			e.printStackTrace();
			return ERR + "Error while resetting the bright biomes list.";
		}
	}

	/* ---------------------------------------------------------------------- Direction  ---------------------------------------------------------------------- */

	/**
	 * Calculates cardinal direction the player is facing.
	 * @return Small message indicating cardinal direction and coordinate
	 *         towards which the player is facing.
	 */
	static String getPlayerDirection(Player player) {
		//-180: Leaning left | +180: Leaning right
		float yaw = player.getLocation().getYaw();
		//Bring to 360 degrees (Clockwise from -X axis)
		if (yaw < 0.0F) {
			yaw += 360.0F;
		}
		//Separate into 8 sectors (Arc: 45deg), offset by 1/2 sector (Arc: 22.5deg)
		int sector = (int) ((yaw + 22.5F) / 45F);
		switch (sector) {
			case 1: return "SW";
			case 2: return "W [-X]";
			case 3: return "NW";
			case 4: return "N [-Z]";
			case 5: return "NE";
			case 6: return "E [+X]";
			case 7: return "SE";
			case 0:
			default: //Example: (359 + 22.5)/45
				return "S [+Z]";
		}
	}

	/* ---------------------------------------------------------------------- Admin ---------------------------------------------------------------------- */

	/** @return How many ticks between each message update. */
	static long getMessageUpdateDelay() {
		return messageUpdateDelay;
	}

	/** @return How many ticks between each biome change check. */
	static long getBiomeUpdateDelay() {
		return biomeUpdateDelay;
	}

	/**
	 * Change how many ticks between each action bar message update.
	 * Values higher than 20 may lead to actionbar text fading before it is updated.
	 * Stops current updater task and schedules a new one with updated value.
	 * @return Small message indicating updated state.
	 */
	static boolean setMessageUpdateDelay(CommandSender sender, String[] args, int argStart) {
		//No number given
		if (args.length < argStart + 1) {
			sendMsg(sender, "Message update delay is currently: "
					+ Util.HLT + Util.getMessageUpdateDelay() + " ticks.");
		}
		//Number was given
		else {
			try {
				long newDelay = Long.parseLong(args[argStart]);

				if (newDelay <= 0 || newDelay > 40) {
					sendMsg(sender, ERR + "Number must be between 1 and 40 ticks.");
					return true;
				}
				messageUpdateDelay = newDelay;

				//Save the new value.
				InfoHUD.getPlugin().getConfig().set(MESSAGE_UPDATE_DELAY_PATH, messageUpdateDelay);
				InfoHUD.getPlugin().saveConfig();

				//Stop plugin and restart with new refresh period.
				InfoHUD.getPlugin().msgSenderTask.cancel();
				InfoHUD.getPlugin().msgSenderTask = InfoHUD.getPlugin().startMessageUpdaterTask(getMessageUpdateDelay());

				sendMsg(sender, "Message update delay set to " + HLT + newDelay + RES + ".");

				return true;
			} catch (NumberFormatException e) {
				sendMsg(sender, Util.ERR + "Must be a positive integer between 1 and 40.");
			}
		}
		return true;
	}

	/** Reloads content of config.yml. */
	static boolean reload(CommandSender sender) {
		boolean success;
		try {
			//Cancel task, reload config, and restart task.
			InfoHUD.getPlugin().msgSenderTask.cancel();
			success = loadConfig();
			InfoHUD.getPlugin().msgSenderTask = InfoHUD.getPlugin().startMessageUpdaterTask(getMessageUpdateDelay());

			if (success) {
				sendMsg(sender, Util.GRN + "Reloaded successfully.");
			}
			else {
				sendMsg(sender, Util.ERR + "Reload failed.");
			}

			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	/** Shortcut to print to server console. */
	static void printToTerminal(String msg) {
		Bukkit.getConsoleSender().sendMessage(SIGNATURE + msg);
	}

	/** Shortcut to printf to server console. */
	static void printToTerminal(String msg, Object ... args) {
		printToTerminal(String.format(msg, args));
	}

	/**
	 * Gets how much time the last update took.
	 * @return Small message indicating status.
	 */
	static boolean getBenchmark(CommandSender sender) {
		sendMsg(sender, "InfoHUD took " + Util.HLT + String.format("%.3f", Util.benchmark / (1000000D)) + Util.RES
				+ " ms (" + Util.HLT + String.format("%.2f", (Util.benchmark / (10000D)) / 50D)
				+ Util.RES + "% tick) during the last tick.");
		return true;
	}

	/** Send chat message to command sender. */
	static void sendMsg(CommandSender sender, String msg) {
		sender.sendMessage(SIGN + "[InfoHUD] " + RES + msg);
	}
}
