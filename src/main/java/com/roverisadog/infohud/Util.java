
package com.roverisadog.infohud;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

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

	public static final String SIGNATURE = Util.SIGN + "[InfoHUD] " + Util.RES;

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
	/* ---------------------------------------------------------------------- Direction  ---------------------------------------------------------------------- */

	/* ---------------------------------------------------------------------- Admin ---------------------------------------------------------------------- */

	/** Shortcut to print to server console. */
	public static void printToTerminal(String msg) {
		Bukkit.getConsoleSender().sendMessage(SIGNATURE + msg);
	}

	/** Shortcut to printf to server console. */
	public static void printToTerminal(String format, Object... args) {
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
