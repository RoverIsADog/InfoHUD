package com.roverisadog.infohud;

import com.roverisadog.infohud.command.CoordMode;
import com.roverisadog.infohud.command.DarkMode;
import com.roverisadog.infohud.command.TimeMode;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class CommandHelper {

	private CommandHelper() {
		throw new IllegalArgumentException();
	}

	protected static boolean setCoordinates(Player p, String[] args, int argsStart) {
		//No argument
		if (args.length < argsStart + 1) {
			Util.sendMsg(p, "Coordinates display is currently set to: " + Util.HLT + PlayerCfg.getCoordinatesMode(p));
			return true;
		}

		//Cycle through all coordinate modes
		for (CoordMode cm : CoordMode.values()) {
			if (cm.name.equalsIgnoreCase(args[argsStart])) {
				Util.sendMsg(p, PlayerCfg.setCoordinatesMode(p, cm));
				return true;
			}
		}

		//Unrecognized argument
		Util.sendMsg(p, "Usage: " + Util.HLT + "'/" +
				Util.CMD_NAME + " " + CommandExecutor.CMD_NORMAL.get(2) + " " + Arrays.toString(CoordMode.values()));
		return true;
	}

	protected static boolean setTime(Player p, String[] args, int argsStart) {
		//No argument
		if (args.length < argsStart + 1) {
			Util.sendMsg(p, "Time display is currently set to: " + Util.HLT + PlayerCfg.getTimeMode(p));
			return true;
		}

		//Cycle through all time modes
		for (TimeMode tm : TimeMode.values()) {
			if (tm.name.equalsIgnoreCase(args[argsStart])) {
				Util.sendMsg(p, PlayerCfg.setTimeMode(p, tm));
				return true;
			}
		}

		//Unrecognized argument
		Util.sendMsg(p, "Usage: " + Util.HLT + "'/" +
				Util.CMD_NAME + " " + CommandExecutor.CMD_NORMAL.get(3) + " " + Arrays.toString(TimeMode.values()));
		return true;
	}

	protected static boolean setDarkMode(Player p, String[] args, int argsStart) {
		//No argument
		if (args.length < argsStart + 1) {
			Util.sendMsg(p, "Dark mode is currently set to: " + Util.HLT + PlayerCfg.getDarkMode(p));
			return true;
		}

		//Cycle through all time modes
		for (DarkMode dm : DarkMode.values()) {
			if (dm.name.equalsIgnoreCase(args[argsStart])) {
				Util.sendMsg(p, PlayerCfg.setDarkMode(p, dm));
				return true;
			}
		}

		//Unrecognized argument
		Util.sendMsg(p, "Usage: " + Util.HLT + "'/" +
				Util.CMD_NAME + " " + CommandExecutor.CMD_NORMAL.get(4) + " " + Arrays.toString(DarkMode.values()));
		return true;
	}

	protected static boolean setBiomes(CommandSender sender, String[] args, int argsStart) {
		//No argument
		if (args.length < argsStart + 1) {
			Util.sendMsg(sender, "Usage: " + Util.HLT + "/" + Util.CMD_NAME + " "
					+ CommandExecutor.CMD_ADMIN.get(3) + " " + CommandExecutor.CMD_BIOMES);
		}
		// [/infohud biome add]
		else if (args[argsStart].equalsIgnoreCase(CommandExecutor.CMD_BIOMES.get(0))) {
			if (args.length < 3) {
				Util.sendMsg(sender, "Enter a biome name.");
			}
			else {
				try {
					//currentBiome
					if ((sender instanceof Player) && args[argsStart + 1].equalsIgnoreCase("here")) {
						Util.sendMsg(sender, Util.addBrightBiome(((Player) sender).getLocation().getBlock().getBiome()));
					}
					//ANY_BIOME
					else {
						Biome b = Biome.valueOf(args[argsStart + 1].toUpperCase());
						Util.sendMsg(sender, Util.addBrightBiome(b));
					}
				} catch (Exception e) {
					Util.sendMsg(sender, Util.ERR
							+ "No biome matching \"" + Util.HLT + args[argsStart + 1].toUpperCase()
							+ Util.ERR + "\" found in version: 1." + Util.apiVersion + " .");
				}
			}
		}
		// [/infohud biome remove]
		else if (args[argsStart].equalsIgnoreCase(CommandExecutor.CMD_BIOMES.get(1))) {
			//No argument
			if (args.length < argsStart + 1) {
				Util.sendMsg(sender, "Enter a biome name.");
			}
			else {
				try {
					//currentBiome
					if ((sender instanceof Player) && args[argsStart + 1].equalsIgnoreCase("here")) {
						Util.sendMsg(sender, Util.removeBrightBiome(((Player) sender).getLocation().getBlock().getBiome()));
					}
					//ANY_BIOME
					else {
						Biome b = Biome.valueOf(args[2].toUpperCase());
						Util.sendMsg(sender, Util.removeBrightBiome(b));
					}
				} catch (Exception e) {
					Util.sendMsg(sender, Util.ERR
							+ "No biome matching \"" + Util.HLT + args[argsStart + 1].toUpperCase()
							+ Util.ERR + "\" found in version: 1." + Util.apiVersion + " .");
				}
			}
		}
		else {
			Util.sendMsg(sender, "Usage: " + Util.HLT + "'/" + Util.CMD_NAME + " "
					+ CommandExecutor.CMD_ADMIN.get(3) + " " + CommandExecutor.CMD_BIOMES);
		}
		return true;
	}
}
