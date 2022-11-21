package com.roverisadog.infohud;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.roverisadog.infohud.config.PlayerCfg;
import com.roverisadog.infohud.config.CoordMode;
import com.roverisadog.infohud.config.DarkMode;
import com.roverisadog.infohud.config.TimeMode;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

/** Handles command completion and execution. */
public class CommandExecutor implements TabExecutor {
	/** Autocomplete choices for normal users. */
	private static final List<String> CMD_NORMAL =
			Arrays.asList("enable", "disable", CoordMode.cmdName, TimeMode.cmdName, DarkMode.cmdName, "help");
	/** Autocomplete choices exclusively for admins. */
	private static final List<String> CMD_ADMIN =
			Arrays.asList("messageUpdateDelay", "reload", "benchmark", "brightBiomes");
	/** All autocomplete choices for admins. */
	private static final List<String> CMD_ALL = Stream
			.concat(CMD_NORMAL.stream(), CMD_ADMIN.stream())
			.collect(Collectors.toList());
	/** Commands relating to bright biome management. */
	private static final List<String> CMD_BIOMES = Arrays.asList("add", "remove", "reset");
	/** List of all biomes in the current minecraft version. */
	private static final List<String> BIOME_LIST = new ArrayList<>();
	static {
		// Load version-specific biomes
		BIOME_LIST.add("here");
		for (Biome b : Biome.values()) {
			BIOME_LIST.add(b.toString());
		}
	}

	/** Instance of the plugin. */
	private final InfoHUD pluginInstance;

	public CommandExecutor(InfoHUD plu) {
		this.pluginInstance = plu;
	}

	/**
	 * Parses and executes a command.
	 * @param sender Source of the command.
	 * @param command Unused
	 * @param label Alias of the command which was used (unused).
	 * @param args Arguments to the command (alias)
	 * @return For now always true (invalid commands generate custom error message)
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		/* DRAFT
		 * if args.length == 0:
		 *     if admin:
		 *         send Usage: CMD_ALL
		 *     else:
		 *         send Usage: CMD
		 * if normalCommand:
		 *     if isPlayer:
		 *         if hasPerm(use):
		 *             try every use commands
		 *         else:
		 *             send error missing perm
		 *     else:
		 *         send error must be user
		 * else if adminCommand:
		 *     if hasPerm(admin) OR isConsole OR is op:
		 *         try every admin commands
		 *     else:
		 *         send error missing perm
		 * else: #Unknown command
		 *     send error unknown command
		 */
		boolean isUser = sender.hasPermission(Util.PERM_USE) || sender.isOp();
		boolean isAdmin = sender.hasPermission(Util.PERM_ADMIN) || sender.isOp();
		boolean isPlayer = (sender instanceof Player);
		boolean isConsole = (sender instanceof ConsoleCommandSender);

		//Illegal sender.
		if (!isPlayer && !isConsole) {
			Util.sendMsg(sender, "Only players and the server console send commands.");
			return true;
		}

		// [/infohud]
		if (args.length == 0) {
			if (isAdmin) {
				Util.sendMsg(sender, "Usage: %s/%s %s", Util.HLT, label, CMD_ALL);
			}
			else {
				Util.sendMsg(sender, "Usage: %s/%s %s", Util.HLT, label, CMD_NORMAL);
			}
		}

		// [/infohud XYZ] Length >= 1, try normal commands
		else if (CMD_NORMAL.contains(args[0])) {

			String argument1 = args[0];
			int currentLevel = 1;

			// [/infohud help]
			if (argument1.equalsIgnoreCase(CMD_NORMAL.get(5))) {
				buildAndSendHelpMenu(sender);
			}

			// All subsequent normal commands are PLAYER ONLY.
			else if (!isPlayer) {
				Util.sendMsg(sender, Util.ERR + "Only players may use this command.");
			}

			// Try player only commands
			else {
				Player p = (Player) sender;

				// Doesn't have infohud.use permission and isn't op.
				if (!isUser) {
					Util.sendMsg(p, Util.ERR + "You do not have the "
							+ Util.HLT + Util.PERM_USE + Util.ERR
							+ " permission to use this commands.");
				}
				// [/infohud enable]
				else if (argument1.equalsIgnoreCase("enable")) {
					pluginInstance.getPlayerConfig().addPlayer(p);
				}
				// [/infohud disable]
				else if (argument1.equalsIgnoreCase("disable")) {
					pluginInstance.getPlayerConfig().removePlayer(p);
				}
				// Further commands require InfoHUD to be enabled before.
				else {
					if (!pluginInstance.getPlayerConfig().isEnabled(p)) {
						Util.sendMsg(p, "Enable InfoHUD first: " + Util.HLT + "/" + label);
					}
					// [/infohud coordinates]
					else if (argument1.equalsIgnoreCase(CoordMode.cmdName)) {
						changeCoordMode(p, args, currentLevel);
					}
					// [/infohud time]
					else if (argument1.equalsIgnoreCase(TimeMode.cmdName)) {
						setTimeMode(p, args, currentLevel);
					}
					// [/infohud darkMode]
					else if (argument1.equalsIgnoreCase(DarkMode.cmdName)) {
						changeDarkMode(p, args, currentLevel);
					}
				}
			}
		}

		// [/infohud XYZ] Length >= 1, try admin commands
		else if (CMD_ADMIN.contains(args[0])) {

			String argument1 = args[0];
			int currentLevel = 1;

			// Can't use admin commands.
			if (!isAdmin && !isConsole) {
				Util.sendMsg(sender, Util.ERR + "You do not have the "
						+ Util.HLT + Util.PERM_ADMIN + Util.ERR
						+ " permission to use this command.");
			}
			// Is admin, try admin commands.
			else {
				// [/infohud messageUpdateDelay]
				if (argument1.equalsIgnoreCase(CMD_ADMIN.get(0))) {
					setMessageUpdateDelay(sender, args, currentLevel);
				}
				// [/infohud reload]
				else if (argument1.equalsIgnoreCase(CMD_ADMIN.get(1))) {
					pluginInstance.reload(sender);
				}
				// [/infohud benchmark]
				else if (argument1.equalsIgnoreCase(CMD_ADMIN.get(2))) {
					printBenchmark(sender);
				}
				// [/infohud brightBiomes]
				else if (argument1.equalsIgnoreCase(CMD_ADMIN.get(3))) {
					changeBrightBiomeList(sender, args, currentLevel);
				}
			}
		}
		else {
			Util.sendMsg(sender, Util.ERR + "Unknown command.");
		}
		return true;
	}

	/** @see TabExecutor#onTabComplete(CommandSender, Command, String, String[])  */
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		boolean isPlayer = sender instanceof Player;
		boolean isConsole = sender instanceof ConsoleCommandSender;
		boolean isUser = sender.hasPermission(Util.PERM_USE) || sender.isOp();
		boolean isAdmin = sender.hasPermission(Util.PERM_ADMIN) || sender.isOp();

		// Does not have infohud.use permission
		if (isPlayer && !isUser) {
			return new ArrayList<>();
		}

		// Fill 1st keyword [/infohud arg1...]
		else if (args.length == 1) {
			String argument1 = args[0];
			if (isConsole || isAdmin) {
				return StringUtil.copyPartialMatches(argument1, CMD_ALL, new ArrayList<>());
			}
			else {
				return StringUtil.copyPartialMatches(argument1, CMD_NORMAL, new ArrayList<>());
			}
		}

		// Fill 2nd keyword [/infohud arg1 arg2...] if the command has a 2nd keyword.
		else if (args.length == 2) {

			String argument1 = args[0];
			String argument2 = args[1];

			// Try normal commands first
			if (isPlayer) {
				//"coordinates"
				if (argument1.equalsIgnoreCase(CoordMode.cmdName)) {
					return StringUtil.copyPartialMatches(argument2, CoordMode.OPTIONS_LIST, new ArrayList<>());
				}
				//"time"
				else if (argument1.equalsIgnoreCase(TimeMode.cmdName)) {
					return StringUtil.copyPartialMatches(argument2, TimeMode.OPTIONS_LIST, new ArrayList<>());
				}
				//"darkMode"
				else if (argument1.equalsIgnoreCase(DarkMode.cmdName)) {
					return StringUtil.copyPartialMatches(argument2, DarkMode.OPTIONS_LIST, new ArrayList<>());
				}
			}

			//Try admin commands if is admin
			if (isAdmin) {
				//"messageUpdateDelay"
				if (argument1.equalsIgnoreCase(CMD_ADMIN.get(0))) {
					return Collections.singletonList(Integer.toString(Util.DEFAULT_MESSAGE_UPDATE_DELAY));
				}
				//"brightBiomes"
				else if (argument1.equalsIgnoreCase(CMD_ADMIN.get(3))) {
					return StringUtil.copyPartialMatches(argument2, CMD_BIOMES, new ArrayList<>());
				}
			}
		}
		// Fill 3rd keyword [/infohud arg1 arg2 arg3...] if the command has a 2nd keyword.
		else if (args.length == 3) {

			String argument1 = args[0];
			String argument2 = args[1];
			String argument3 = args[2];

			// "brightBiomes"
			if (argument1.equalsIgnoreCase(CMD_ADMIN.get(3))) {
				// "brightBiomes add"
				if (argument2.equalsIgnoreCase(CMD_BIOMES.get(0))) {
					return StringUtil.copyPartialMatches(argument3, BIOME_LIST, new ArrayList<>());
				}
				// "brightBiomes remove"
				else if (argument2.equalsIgnoreCase(CMD_BIOMES.get(1))) {
					List<String> temp = Util.getBrightBiomesList();
					temp.add("here");
					return StringUtil.copyPartialMatches(argument3, temp, new ArrayList<>());
				}
			}
		}
		// Unknown command being typed
		return new ArrayList<>();
	}

	/**
	 * Sets the coordinates mode of a player.
	 * @param p Concerned player.
	 * @param args Arguments list
	 * @param argsStart Commands before argsStart have been consumed.
	 */
	private static void changeCoordMode(Player p, String[] args, int argsStart) {
		// No argument
		if (args.length < argsStart + 1) {
			Util.sendMsg(p, "Coordinates display is currently set to: " + Util.HLT + InfoHUD.getPlugin().getPlayerConfig().getCoordinatesMode(p));
			return;
		}

		//Cycle through all coordinate modes
		for (CoordMode cm : CoordMode.values()) {
			if (cm.name.equalsIgnoreCase(args[argsStart])) {
				Util.sendMsg(p, InfoHUD.getPlugin().getPlayerConfig().setCoordinatesMode(p, cm));
				return;
			}
		}

		// Unrecognised argument
		Util.sendMsg(p, "Usage: " + Util.HLT + "'/" +
				Util.CMD_NAME + " " + CoordMode.cmdName + " " + Arrays.toString(CoordMode.values()));
	}

	/**
	 * Changes the time mode of a player.
	 * @param p Concerned player.
	 * @param args Arguments list.
	 * @param argsStart Commands before argsStart have been consumed.
	 */
	private static void setTimeMode(Player p, String[] args, int argsStart) {
		// No argument
		if (args.length < argsStart + 1) {
			Util.sendMsg(p, "Time display is currently set to: " + Util.HLT + InfoHUD.getPlugin().getPlayerConfig().getTimeMode(p));
			return;
		}

		// Cycle through all time modes
		for (TimeMode tm : TimeMode.values()) {
			if (tm.name.equalsIgnoreCase(args[argsStart])) {
				Util.sendMsg(p, InfoHUD.getPlugin().getPlayerConfig().setTimeMode(p, tm));
				return;
			}
		}

		// Unrecognised argument
		Util.sendMsg(p, "Usage: " + Util.HLT + "'/" +
				Util.CMD_NAME + " " + TimeMode.cmdName + " " + Arrays.toString(TimeMode.values()));
	}

	private static void changeDarkMode(Player p, String[] args, int argsStart) {
		//No argument
		if (args.length < argsStart + 1) {
			Util.sendMsg(p, "Dark mode is currently set to: " + Util.HLT + InfoHUD.getPlugin().getPlayerConfig().getDarkMode(p));
			return;
		}

		// Cycle through all time modes
		for (DarkMode dm : DarkMode.values()) {
			if (dm.name.equalsIgnoreCase(args[argsStart])) {
				Util.sendMsg(p, InfoHUD.getPlugin().getPlayerConfig().setDarkMode(p, dm));
				return;
			}
		}

		// Unrecognised argument
		Util.sendMsg(p, "Usage: " + Util.HLT + "'/" +
				Util.CMD_NAME + " " + DarkMode.cmdName + " " + Arrays.toString(DarkMode.values()));
	}

	/**
	 * Updates the bright biomes list.
	 * @param sender Sender who initiated the command.
	 * @param args Arguments list.
	 * @param argsStart Commands before argsStart have been consumed.
	 */
	private static void changeBrightBiomeList(CommandSender sender, String[] args, int argsStart) {
		// No argument
		if (args.length < argsStart + 1) {
			Util.sendMsg(sender, "Usage: " + Util.HLT + "/" + Util.CMD_NAME + " "
					+ CMD_ADMIN.get(3) + " " + CMD_BIOMES);
			return;
		}

		String option = args[argsStart];
		// [/infohud biome add]
		if (option.equalsIgnoreCase(CMD_BIOMES.get(0))) {
			// No argument
			if (args.length < argsStart + 2) {
				Util.sendMsg(sender, "Enter the name of the biome to add.");
			}
			else {
				String biomeName = args[argsStart + 1];
				try {
					// Add the CURRENT biome
					if ((sender instanceof Player) && biomeName.equalsIgnoreCase("here")) {
						Util.sendMsg(sender, Util.addBrightBiome(((Player) sender).getLocation().getBlock().getBiome()));
					}
					// Add some other biome
					else {
						Biome b = Biome.valueOf(biomeName.toUpperCase());
						Util.sendMsg(sender, Util.addBrightBiome(b));
					}
				} catch (IllegalArgumentException e) {
					Util.sendMsg(sender, Util.ERR
							+ "No biome matching \"" + Util.HLT + args[argsStart + 1].toUpperCase()
							+ Util.ERR + "\" found in version: 1." + Util.apiVersion + " .");
				}
			}
		}
		// [/infohud biome remove]
		else if (option.equalsIgnoreCase(CMD_BIOMES.get(1))) {
			// No argument
			if (args.length < argsStart + 2) {
				Util.sendMsg(sender, "Enter the name of the biome to remove.");
			}
			else {
				String biomeName = args[argsStart + 1];
				try {
					// Remove the CURRENT biome
					if ((sender instanceof Player) && biomeName.equalsIgnoreCase("here")) {
						Util.sendMsg(sender, Util.removeBrightBiome(((Player) sender).getLocation().getBlock().getBiome()));
					}
					// Remove some other biome
					else {
						Biome b = Biome.valueOf(biomeName.toUpperCase());
						Util.sendMsg(sender, Util.removeBrightBiome(b));
					}
				} catch (Exception e) {
					Util.sendMsg(sender, Util.ERR
							+ "No biome matching \"" + Util.HLT + biomeName.toUpperCase()
							+ Util.ERR + "\" found in version: 1." + Util.apiVersion + " .");
				}
			}
		}
		// [/infohud biome reset]
		else if (args[argsStart].equalsIgnoreCase(CMD_BIOMES.get(2))) {
			Util.sendMsg(sender, Util.resetBrightBiomes());
		}
		else {
			Util.sendMsg(sender, "Usage: " + Util.HLT + "'/" + Util.CMD_NAME + " "
					+ CMD_ADMIN.get(3) + " " + CMD_BIOMES);
		}
	}

	/**
	 * Change how many ticks between each action bar message update. Values higher than 20
	 * may lead to actionbar text fading before the next update. Stops current updater
	 * task and schedules a new one with the updated value.
	 * @param sender Concerned sender.
	 * @param args Arguments list.
	 * @param argStart Commands before argStart have been consumed.
	 */
	private void setMessageUpdateDelay(CommandSender sender, String[] args, int argStart) {
		//No number given
		if (args.length < argStart + 1) {
			Util.sendMsg(sender, "Message update delay is currently: "
					+ Util.HLT + Util.getMessageUpdateDelay() + " ticks.");
		}

		//Number was given
		else {
			try {
				long newDelay = Long.parseLong(args[argStart]);

				if (newDelay <= 0 || newDelay > 40) {
					Util.sendMsg(sender, Util.ERR + "Number must be between 1 and 40 ticks.");
					return;
				}
				Util.messageUpdateDelay = newDelay;

				// Save the new value into config.
				InfoHUD.getPlugin().getConfig().set(Util.MESSAGE_UPDATE_DELAY_PATH, newDelay);
				InfoHUD.getPlugin().saveConfig();

				// Stop plugin and restart with new refresh period.
				InfoHUD.getPlugin().msgSenderTask.cancel();
				InfoHUD.getPlugin().msgSenderTask = InfoHUD.getPlugin().startMessageUpdaterTask(newDelay);

				Util.sendMsg(sender, "Message update delay set to " + Util.HLT + newDelay + Util.RES + ".");
			} catch (NumberFormatException e) {
				Util.sendMsg(sender, Util.ERR + "Must be a positive integer between 1 and 40.");
			}
		}
	}

	/**
	 * Prints how long the last task iteration took.
	 * @param sender Sender who issued the command.
	 */
	private void printBenchmark(CommandSender sender) {
		Util.sendMsg(sender, "InfoHUD took " + Util.HLT + String.format("%.3f", Util.benchmark / (1000000D)) + Util.RES
				+ " ms (" + Util.HLT + String.format("%.2f", (Util.benchmark / (10000D)) / 50D)
				+ Util.RES + "% tick) during the last tick.");
	}

	/**
	 * Builds a customised help menu depending on the sender's configuration and permissions
	 * and sends it to the sender.
	 * @param sender Sender requesting the help menu.
	 */
	private void buildAndSendHelpMenu(CommandSender sender) {
		List<String> msg = new ArrayList<>();
		msg.add("============ " + Util.HLT + "InfoHUD " + pluginInstance.getDescription().getVersion() + " on "
				+ Util.serverVendor + " 1." + Util.apiVersion + Util.RES + " ============");

		// Display current player's settings.
		if (sender instanceof Player) {
			Player p = (Player) sender;
			msg.add((InfoHUD.getPlugin().getPlayerConfig().isEnabled(p) ? Util.GRN + "Enabled" : Util.ERR + "Disabled")
					+ Util.RES + " for " + p.getDisplayName()
					+ (p.hasPermission(Util.PERM_ADMIN) || p.isOp()
					? Util.GRN + " (InfoHUD Admin)" : ""));

			if (InfoHUD.getPlugin().getPlayerConfig().isEnabled(p)) {
				PlayerCfg cfg = InfoHUD.getPlugin().getPlayerConfig().getConfig(p);
				msg.add(Util.HLT + "   coordinates: " + Util.RES + cfg.getCoordMode().description);
				msg.add(Util.HLT + "   time: " + Util.RES + cfg.getTimeMode().description);
				msg.add(Util.HLT + "   darkMode: " + Util.RES + cfg.getDarkMode().description);
			}
			msg.add("");
		}

		// Display normal user commands.
		msg.add(Util.GRN + "Settings");
		if (sender instanceof Player) {
			msg.add(Util.HLT + ">coordinates: " + Util.RES + "Enable/Disable coordinates display.");
			msg.add(Util.HLT + ">time: " + Util.RES + "Time display format (or disable).");
			msg.add(Util.HLT + ">darkMode: " + Util.RES + "Enable/Disable/Auto using lighter colors.");
		}

		// Display admin commands.
		if (sender.hasPermission(Util.PERM_ADMIN) || sender instanceof ConsoleCommandSender || sender.isOp()) {
			msg.add(Util.HLT + ">messageUpdateDelay: "
					+ Util.RES + "Ticks between each refresh.");
			msg.add(Util.HLT + ">reload: "
					+ Util.RES + "Reloads config.yml. " + Util.ERR + "(some recent changes may be lost).");
			msg.add(Util.HLT + ">benchmark: "
					+ Util.RES + "Check how long the last refresh took (1 tick = 50ms).");
			msg.add(Util.HLT + ">brightBiomes: "
					+ Util.RES + "Add/Remove biomes where dark mode can turns on automatically.");
		}

		String[] msgArr = new String[msg.size()];
		sender.sendMessage(msg.toArray(msgArr));
	}

}
