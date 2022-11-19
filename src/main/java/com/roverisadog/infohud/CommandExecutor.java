package com.roverisadog.infohud;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.roverisadog.infohud.command.CoordMode;
import com.roverisadog.infohud.command.DarkMode;
import com.roverisadog.infohud.command.TimeMode;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.StringUtil;

/** Handles command completion and execution. */
public class CommandExecutor implements TabExecutor {
	//Autocomplete choices
	protected static final List<String> CMD_NORMAL =
			Arrays.asList("enable", "disable", CoordMode.cmdName, TimeMode.cmdName, DarkMode.cmdName, "help");
	protected static final List<String> CMD_ADMIN =
			Arrays.asList("messageUpdateDelay", "reload", "benchmark", "brightBiomes");
	protected static final List<String> ALL_CMD = Stream.concat(CMD_NORMAL.stream(), CMD_ADMIN.stream())
			.collect(Collectors.toList());

	protected static final List<String> CMD_BIOMES = Arrays.asList("add", "remove", "reset");
	protected static final List<String> BIOME_LIST = new ArrayList<>();

	/** Instance of the plugin. */
	private final Plugin plugin;

	static {
		// Load version-specific biomes
		BIOME_LIST.add("here");
		for (Biome b : Biome.values()) {
			BIOME_LIST.add(b.toString());
		}
	}

	public CommandExecutor(Plugin plu) {
		this.plugin = plu;
	}

	/**
	 * Carries out commands.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        /*
        if args.length == 0:
            if admin:
                send Usage: concat(CMD + CMD_ADMIN)
            else:
                send Usage: CMD
        if normalCommand:
            if isPlayer:
                if hasPerm(use):
                    try every use commands
                else:
                    send error missing perm
            else:
                send error must be user
        else if adminCommand:
            if hasPerm(admin) OR isConsole:
                try every admin commands
            else:
                send error missing perm
        else: #Unknown command
            send error unknown command
         */

		boolean canUse = sender.hasPermission(Util.PERM_USE) || sender.isOp();
		boolean isAdmin = sender.hasPermission(Util.PERM_ADMIN) || sender.isOp();
		boolean isPlayer = (sender instanceof Player);
		boolean isConsole = (sender instanceof ConsoleCommandSender);

		//Illegal sender.
		if (!isPlayer && !isConsole) {
			Util.sendMsg(sender, "Only players and the console may use commands.");
			return true;
		}

		// [/infohud]
		if (args.length == 0) {
			if (isAdmin) {
				Util.sendMsg(sender, "Usage: " + Util.HLT + "/" + Util.CMD_NAME + " " +
						Stream.concat(CMD_NORMAL.stream(), CMD_ADMIN.stream())
								.collect(Collectors.toList()));
			}
			else {
				Util.sendMsg(sender, "Usage: " + Util.HLT + "/" + Util.CMD_NAME + " " + CMD_NORMAL);
			}
			return true;
		}

		//Length >= 1, try normal commands
		else if (CMD_NORMAL.contains(args[0])) {

			String argument1 = args[0];
			int currentLevel = 1;

			// [/infohud help]
			if (argument1.equalsIgnoreCase(CMD_NORMAL.get(5))) {
				buildHelpMenu(sender);
				return true;
			}
			//All subsequent normal commands PLAYER ONLY.
			else if (!(sender instanceof Player)) {
				Util.sendMsg(sender, Util.ERR + "Only players may use this command.");
				return true;
			}
			//Try player only commands
			else {
				Player p = (Player) sender;

				//Doesn't have infohud.use permission.
				if (!canUse) {
					Util.sendMsg(p, Util.ERR + "You do not have the " + Util.HLT
							+ Util.PERM_USE + Util.ERR + " permission to use this commands.");
					return true;
				}
				// [/infohud enable]
				else if (argument1.equalsIgnoreCase(CMD_NORMAL.get(0))) {
					return PlayerCfg.savePlayer(p);
				}
				// [/infohud disable]
				else if (argument1.equalsIgnoreCase(CMD_NORMAL.get(1))) {
					return PlayerCfg.removePlayer(p);
				}
				//Further commands require InfoHUD to be enabled first.
				else if (!PlayerCfg.isEnabled(p)) {
					Util.sendMsg(p, "Enable InfoHUD with "
							+ Util.HLT + "/" + Util.CMD_NAME + " " + CMD_NORMAL.get(0) + Util.RES + " first.");
					return true;
				}
				else {
					// [/infohud coordinates]
					if (argument1.equalsIgnoreCase(CMD_NORMAL.get(2))) {
						return CommandHelper.setCoordinates(p, args, currentLevel);
					}
					// [/infohud time]
					else if (argument1.equalsIgnoreCase(CMD_NORMAL.get(3))) {
						return CommandHelper.setTime(p, args, currentLevel);
					}
					// [/infohud darkMode]
					else if (argument1.equalsIgnoreCase(CMD_NORMAL.get(4))) {
						return CommandHelper.setDarkMode(p, args, currentLevel);
					}
				}
				return true;
			}
		}

		//Length >= 1, try normal admin commands
		else if (CMD_ADMIN.contains(args[0])) {

			String argument1 = args[0];
			int currentLevel = 1;

			//Can't use admin commands.
			if (!isAdmin && !isConsole) {
				Util.sendMsg(sender, Util.ERR + "You do not have the " + Util.HLT
						+ Util.PERM_ADMIN + Util.ERR + " permission to use this command.");
				return true;
			}
			//Is admin, try admin commands.
			else {
				// [/infohud messageUpdateDelay]
				if (argument1.equalsIgnoreCase(CMD_ADMIN.get(0))) {
					return Util.setMessageUpdateDelay(sender, args, currentLevel);
				}
				// [/infohud reload]
				else if (argument1.equalsIgnoreCase(CMD_ADMIN.get(1))) {
					return Util.reload(sender);
				}
				// [/infohud benchmark]
				else if (argument1.equalsIgnoreCase(CMD_ADMIN.get(2))) {
					return Util.getBenchmark(sender);
				}
				// [/infohud brightBiomes]
				else if (argument1.equalsIgnoreCase(CMD_ADMIN.get(3))) {
					return CommandHelper.setBiomes(sender, args, currentLevel);
				}
			}
		}
		else {
			Util.sendMsg(sender, Util.ERR + "Unknown command.");
		}
		return true;
	}

	/** Tab completer. */
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		boolean isPlayer = sender instanceof Player;
		boolean isConsole = sender instanceof ConsoleCommandSender;
		boolean canUse = sender.hasPermission(Util.PERM_USE) || sender.isOp();
		boolean isAdmin = sender.hasPermission(Util.PERM_ADMIN) || sender.isOp();

		//Does not have infohud.use permission
		if (isPlayer && !canUse) {
			return new ArrayList<>();
		}

		//1st keyword [/infohud arg1[...]]
		else if (args.length == 1) {
			String argument1 = args[0];
			if (isConsole || isAdmin) {
				return StringUtil.copyPartialMatches(argument1, ALL_CMD, new ArrayList<>());
			}
			else {
				return StringUtil.copyPartialMatches(argument1, CMD_NORMAL, new ArrayList<>());
			}
		}

		//2nd keyword [/infohud arg1 arg2[...]]
		else if (args.length == 2) {

			String argument1 = args[0];
			String argument2 = args[1];

			//Try normal commands
			if (isPlayer) {
				//"coordinates"
				if (CoordMode.cmdName.equalsIgnoreCase(argument1)) {
					return StringUtil.copyPartialMatches(argument2, CoordMode.OPTIONS_LIST, new ArrayList<>());
				}
				//"time"
				else if (TimeMode.cmdName.equalsIgnoreCase(argument1)) {
					return StringUtil.copyPartialMatches(argument2, TimeMode.OPTIONS_LIST, new ArrayList<>());
				}
				//"darkMode"
				else if (DarkMode.cmdName.equalsIgnoreCase(argument1)) {
					return StringUtil.copyPartialMatches(argument2, DarkMode.OPTIONS_LIST, new ArrayList<>());
				}
			}

			//Try admin commands if is admin
			if (isAdmin) {
				//"refreshRate"
				if (argument1.equalsIgnoreCase(CMD_ADMIN.get(0))) {
					return Collections.singletonList(String.valueOf(Util.DEFAULT_MESSAGE_UPDATE_DELAY));
				}
				//"brightBiomes"
				else if (argument1.equalsIgnoreCase(CMD_ADMIN.get(3))) {
					return StringUtil.copyPartialMatches(args[1], CMD_BIOMES, new ArrayList<>());
				}
			}
		}
		//3rd keyword [/infohud arg1 arg2 arg3[...]]
		else if (args.length == 3) {

			String argument1 = args[0];
			String argument2 = args[1];
			String argument3 = args[2];

			//"brightBiomes"
			if (argument1.equalsIgnoreCase(CMD_ADMIN.get(3))) {
				//"brightBiomes add"
				if (argument2.equalsIgnoreCase(CMD_BIOMES.get(0))) {
					return StringUtil.copyPartialMatches(argument3, BIOME_LIST, new ArrayList<>());
				}
				//"brightBiomes remove"
				else if (argument2.equalsIgnoreCase(CMD_BIOMES.get(1))) {
					List<String> temp = Util.getBrightBiomesList();
					temp.add("here");
					return StringUtil.copyPartialMatches(argument3, temp, new ArrayList<>());
				}
			}
		}
		//Unrecognized
		return new ArrayList<>();
	}

	private void buildHelpMenu(CommandSender sender) {
		List<String> msg = new ArrayList<>();
		msg.add("============ " + Util.HLT + "InfoHUD " + plugin.getDescription().getVersion() + " on "
				+ Util.serverVendor + " 1." + Util.apiVersion + Util.RES + " ============");

		//Display current player's settings.
		if (sender instanceof Player) {
			Player p = (Player) sender;
			msg.add((PlayerCfg.isEnabled(p) ? Util.GRN + "Enabled" : Util.ERR + "Disabled")
					+ Util.RES + " for " + p.getDisplayName()
					+ (p.hasPermission(Util.PERM_ADMIN) || p.isOp()
					? Util.GRN + " (InfoHUD Admin)" : ""));

			if (PlayerCfg.isEnabled(p)) {
				PlayerCfg cfg = PlayerCfg.getConfig(p);
				msg.add(Util.HLT + "   coordinates: " + Util.RES + cfg.coordMode.description);
				msg.add(Util.HLT + "   time: " + Util.RES + cfg.timeMode.description);
				msg.add(Util.HLT + "   darkMode: " + Util.RES + cfg.darkMode.description);
			}
			msg.add("");
		}

		//Display normal user commands.
		msg.add(Util.GRN + "Settings");
		if (sender instanceof Player) {
			msg.add(Util.HLT + ">coordinates: " + Util.RES + "Enable/Disable coordinates display.");
			msg.add(Util.HLT + ">time: " + Util.RES + "Time display format (or disable).");
			msg.add(Util.HLT + ">darkMode: " + Util.RES + "Enable/Disable/Auto using lighter colors.");
		}

		//Display admin commands.
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
