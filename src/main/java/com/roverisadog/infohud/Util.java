
package com.roverisadog.infohud;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
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
	public static EnumSet<Biome> brightBiomes;
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
	public static long messageUpdateDelay;
	/** Delay between each biome detection update. */
	public static long biomeUpdateDelay = 40L;

	static long benchmark = 0;

	/**
	 * Utility method that attempts to get a ChatColor from a given name.
	 * @param name Name to check.
	 * @return The corresponding ChatColor.
	 * @throws NullPointerException If the colour is not found.
	 * @see <a href="https://minecraft.gamepedia.com/Formatting_codes">List of colour codes</a>
	 */
	public static ChatColor getColor(String name) {
		for (ChatColor col : ChatColor.values()) {
			if (col.name().equalsIgnoreCase(name)) {
				return col;
			}
		}
		printToTerminal("%s");
		throw new NullPointerException(String.format("%sCould not find colour named \"%s\".", Util.ERR, name));
	}

	/* ---------------------------------------------------------------------- Dark Mode ---------------------------------------------------------------------- */

	/**
	 * Get a newly created list of all bright biomes currently recognized. Not the
	 * same as all bright biomes on the config file as some may not be recognized
	 * by older/newer minecraft versions.
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

	/** Shortcut to print to server console. */
	public static void printToTerminal(String msg) {
		Bukkit.getConsoleSender().sendMessage(SIGNATURE + msg);
	}

	/** Shortcut to printf to server console. */
	static void printToTerminal(String format, Object... args) {
		printToTerminal(String.format(format, args));
	}

	/** Send chat message to command sender. */
	public static void sendMsg(CommandSender sender, String msg) {
		sender.sendMessage(SIGN + "[InfoHUD] " + RES + msg);
	}

	public static void sendMsg(CommandSender sender, String format, Object... args) {
		sendMsg(sender, String.format(format, args));
	}

}
