
package com.roverisadog.infohud;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.StringUtil;

/** Handles command completion and execution. */
public class CommandHandler implements TabExecutor {
    //Autocomplete choices
    private static final List<String> CMD = Arrays.asList("enable", "disable", "coordinates", "time", "darkMode", "help");
    private static final List<String> CMD_ADMIN = Arrays.asList("refreshRate", "reload", "benchmark", "brightBiomes");
    private static final List<String> CMD_COORD = Arrays.asList("disabled", "enabled");
    private static final List<String> CMD_TIME = Arrays.asList("disabled", "currentTick", "clock", "villagerSchedule");
    private static final List<String> CMD_DARK = Arrays.asList("disabled", "enabled", "auto");
    private static final List<String> CMD_BIOMES = Arrays.asList("add", "remove");
    private static final List<String> BIOME_LIST = new ArrayList<>();

    /** Instance of the plugin. */
    private final Plugin plu;

    public CommandHandler(Plugin plu) {
        for (Biome b : Biome.values()) BIOME_LIST.add(b.toString());
        BIOME_LIST.add("here");
        this.plu = plu;
    }

    /**
     * Carries out commands.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        /*
        if args.length == 0:
            if admin:
                send usage (CMD + CMD_ADMIN)
            else:
                send usage CMD
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
        if (!(sender instanceof Player) && !(sender instanceof ConsoleCommandSender)) {
            sendMsg(sender, "Only players and the console may use commands.");
            return true;
        }

        else if (args.length == 0) {
            if (sender.hasPermission(Util.PERM_ADMIN))
                sendMsg(sender, "Usage: " + Util.HLT + "/" + Util.CMD_NAME + " " + Stream.concat(CMD.stream(), CMD_ADMIN.stream()).collect(Collectors.toList()) .toString());
            else
                sendMsg(sender, "Usage: " + Util.HLT + "/" + Util.CMD_NAME + " " + CMD.toString());
            return true;
        }

        //Is normal command
        else if (CMD.contains(args[0])) {

            //"help"
            if (args[0].equalsIgnoreCase(CMD.get(5))) {
                buildHelpMenu(sender);
                return true;
            }

            //Not player
            if (!(sender instanceof Player)) {
                sendMsg(sender, Util.ERR + "Only players may use this command.");
                return true;
            }
            Player p = (Player) sender;

            //No permission
            if (!p.hasPermission(Util.PERM_USE)) {
                sendMsg(p, Util.ERR + "You do not have the " + Util.HLT + Util.PERM_USE + Util.ERR + " permission to use this commands.");
                return true;
            }

            boolean enabled = Util.isEnabled(p);

            //"enable"
            if (args[0].equalsIgnoreCase(CMD.get(0))) {
                if (!enabled) sendMsg(p, Util.savePlayer(p));
                else sendMsg(p, Util.HLT + "InfoHUD was already enabled.");
                return true;
            }
            //"disable"
            else if (args[0].equalsIgnoreCase(CMD.get(1))) {
                if (enabled) sendMsg(p, Util.removePlayer(p));
                else sendMsg(p, Util.HLT + "InfoHUD was already disabled.");
                return true;
            }

            else if (!enabled) {
                sendMsg(p, "Enable InfoHUD with " + Util.HLT + "/" + Util.CMD_NAME + " " + CMD.get(0) + Util.RES + " first.");
                return true;
            }

            //"coordinates"
            else if (args[0].equalsIgnoreCase(CMD.get(2))) {
                //No argument
                if (args.length < 2) {
                    sendMsg(p, "Coordinates display is currently set to: " + Util.HLT + Util.COORDS_OPTIONS[Util.getCoordinatesMode(p)]);
                    return true;
                }
                //Cycle through all coordinate modes
                for (int i = 0; i < CMD_COORD.size(); i++) {
                    if (args[1].equalsIgnoreCase(CMD_COORD.get(i))) {
                        sendMsg(p, Util.setCoordinatesMode(p, i));
                        return true;
                    }
                }
                //Unrecognized argument
                sendMsg(p, "Usage: " + Util.HLT + "'/" + Util.CMD_NAME + " " + CMD.get(2) + " " + CMD_COORD.toString());
                return false;
            }
            //"time"
            else if (args[0].equalsIgnoreCase(CMD.get(3))) {
                //No argument
                if (args.length < 2) {
                    sendMsg(p, "Time display is currently set to: " + Util.HLT + Util.TIME_OPTIONS[Util.getTimeMode(p)]);
                    return true;
                }
                //Cycle through all time modes
                for (int i = 0; i < CMD_TIME.size(); i++) {
                    if (args[1].equalsIgnoreCase(CMD_TIME.get(i))) {
                        sendMsg(p, Util.setTimeMode(p, i));
                        return true;
                    }
                }
                //Unrecognized argument
                sendMsg(p, "Usage: " + Util.HLT + "'/" + Util.CMD_NAME + " " + CMD.get(3) + " " + CMD_TIME.toString());
            }
            //"darkMode"
            else if (args[0].equalsIgnoreCase(CMD.get(4))) {
                //No argument
                if (args.length < 2) {
                    sendMsg(p, "Dark mode is currently set to: " + Util.HLT + Util.DARK_OPTIONS[Util.getDarkMode(p)]);
                    return true;
                }
                //Cycle through all dark modes
                for (int i = 0; i < CMD_DARK.size(); i++) {
                    if (args[1].equalsIgnoreCase(CMD_DARK.get(i))) {
                        sendMsg(p, Util.setDarkMode(p, i));
                        return true;
                    }
                }
                //Unrecognized argument
                sendMsg(p, "Usage: " + Util.HLT + "'/" + Util.CMD_NAME + " " + CMD.get(4) + " " + CMD_DARK.toString());
            }
            return true;
        }

        //Admin command
        else if (CMD_ADMIN.contains(args[0])) {
            //Doesn't have permission
            if (!sender.hasPermission(Util.PERM_ADMIN) && !(sender instanceof ConsoleCommandSender)) {
                sendMsg(sender, Util.ERR + "You do not have the " + Util.HLT + Util.PERM_ADMIN + Util.ERR + " permission to use this command.");
                return true;
            }

            //"refreshRate"
            if (args[0].equalsIgnoreCase(CMD_ADMIN.get(0))) {
                if (args.length < 2) sendMsg(sender, "Refresh rate is currently: " + Util.HLT + Util.getRefreshRate() + " ticks.");
                else {
                    try { sendMsg(sender, Util.setRefreshRate(Long.parseLong(args[1]))); }
                    catch (NumberFormatException e) { sendMsg(sender, Util.ERR + "This command only accepts positive integers between 1 and 40."); }
                }
            }
            //"reload"
            else if (args[0].equalsIgnoreCase(CMD_ADMIN.get(1))) {
                if (args.length > 1) sendMsg(sender, Util.ERR + "This command does not take arguments.");
                else sendMsg(sender, Util.reload() ? Util.GREN + "Reloaded successfully." : Util.ERR + "Reload failed.");
            }
            //"benchmark"
            else if (args[0].equalsIgnoreCase(CMD_ADMIN.get(2))) {
                if (args.length > 1) sendMsg(sender, Util.ERR + "This command does not take arguments.");
                else sendMsg(sender, Util.getBenchmark());
            }
            //"brightBiomes"
            else if (args[0].equalsIgnoreCase(CMD_ADMIN.get(3))) {
                //No argument
                if (args.length < 2) sendMsg(sender, "Usage: " + Util.HLT + "'/" + Util.CMD_NAME + " " + CMD_ADMIN.get(3) + " " + CMD_BIOMES.toString());
                //"add"
                else if (args[1].equalsIgnoreCase(CMD_BIOMES.get(0))) {
                    if (args.length < 3) sendMsg(sender, "Enter a biome name.");
                    else {
                        try {
                            //currentBiome
                            if ((sender instanceof Player) && args[2].equalsIgnoreCase("here")) {
                                sendMsg(sender, Util.addBrightBiome(((Player) sender).getLocation().getBlock().getBiome()));
                                return true;
                            }
                            //ANY_BIOME
                            Biome b = Biome.valueOf(args[2].toUpperCase());
                            sendMsg(sender, Util.addBrightBiome(b));
                        } catch (Exception e) {
                            sendMsg(sender, Util.ERR + "No biome matching \"" + Util.HLT + args[2].toUpperCase() + Util.ERR + "\" in version: 1." + Util.apiVersion + " found.");
                        }
                    }
                } //"remove"
                else if (args[1].equalsIgnoreCase(CMD_BIOMES.get(1))) {
                    //No argument
                    if (args.length < 3) sendMsg(sender, "Enter a biome name.");
                    else {
                        try {
                            //currentBiome
                            if ((sender instanceof Player) && args[2].equalsIgnoreCase("here")) {
                                sendMsg(sender, Util.removeBrightBiome(((Player) sender).getLocation().getBlock().getBiome()));
                                return true;
                            }
                            //ANY_BIOME
                            Biome b = Biome.valueOf(args[2].toUpperCase());
                            sendMsg(sender, Util.removeBrightBiome(b));
                        } catch (Exception e) {
                            sendMsg(sender, Util.ERR + "No biome matching \"" + Util.HLT + args[2].toUpperCase() + Util.ERR + "\" in version: 1." + Util.apiVersion + " found.");
                        }
                    }
                }
                else sendMsg(sender, "Usage: " + Util.HLT + "'/" + Util.CMD_NAME + " " + CMD_ADMIN.get(3) + " " + CMD_BIOMES.toString());
            }
        }
        else {
            sendMsg(sender, Util.ERR + "Unknown command.");
        }
        return true;
    }

    /** Tab completer. */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean isPlayer = sender instanceof Player;
        boolean isConsole = sender instanceof ConsoleCommandSender;

        if (isPlayer && !sender.hasPermission(Util.PERM_USE)) {
            return new ArrayList<>();
        }

        else if (args.length == 1) {
            if (isConsole || sender.hasPermission(Util.PERM_ADMIN)) {
                return StringUtil.copyPartialMatches(args[0], Stream.concat(CMD.stream(), CMD_ADMIN.stream()).collect(Collectors.toList()), new ArrayList<>());
            }
            else {
                return StringUtil.copyPartialMatches(args[0], CMD, new ArrayList<>());
            }
        }
        else if (args.length == 2) {
            //"coordinates"
            if (args[0].equalsIgnoreCase(CMD.get(2))) {
                return (isPlayer && sender.hasPermission(Util.PERM_USE)) ? StringUtil.copyPartialMatches(args[1], CMD_COORD, new ArrayList<>()) : new ArrayList<>();
            }
            //"time"
            else if (args[0].equalsIgnoreCase(CMD.get(3))) {
                return (isPlayer && sender.hasPermission(Util.PERM_USE)) ? StringUtil.copyPartialMatches(args[1], CMD_TIME, new ArrayList<>()) : new ArrayList<>();
            }
            //"darkMode"
            else if (args[0].equalsIgnoreCase(CMD.get(4))) {
                return (isPlayer && sender.hasPermission(Util.PERM_USE)) ? StringUtil.copyPartialMatches(args[1], CMD_DARK, new ArrayList<>()) : new ArrayList<>();
            }
            //"refreshRate"
            else if (args[0].equalsIgnoreCase(CMD_ADMIN.get(0))) {
                return (sender.hasPermission(Util.PERM_ADMIN)) ? Collections.singletonList("5") : new ArrayList<>();
            }
            //"brightBiomes"
            else if (args[0].equalsIgnoreCase(CMD_ADMIN.get(3))) {
                return (sender.hasPermission(Util.PERM_ADMIN)) ? StringUtil.copyPartialMatches(args[1], CMD_BIOMES, new ArrayList<>()) : new ArrayList<>();
            }
        }
        else if (args.length == 3) {
            //"brightBiomes"
            if (args[0].equalsIgnoreCase(CMD_ADMIN.get(3))) {
                //"brightBiomes add"
                if (args[1].equalsIgnoreCase(CMD_BIOMES.get(0))) {
                    return StringUtil.copyPartialMatches(args[2], BIOME_LIST, new ArrayList<>());
                }
                //"brightBiomes remove"
                else if (args[1].equalsIgnoreCase(CMD_BIOMES.get(1))) {
                    List<String> temp = Util.getBrightBiomesList();
                    temp.add("here");
                    return StringUtil.copyPartialMatches(args[2], temp, new ArrayList<>());
                }
            }
        }
        return new ArrayList<>();
    }

    /** Send chat message to command sender. */
    private void sendMsg(CommandSender sender, String msg) {
        sender.sendMessage(Util.SIGNATURE + "[InfoHUD] " + Util.RES + msg);
    }

    private void buildHelpMenu(CommandSender sender) {
        List<String> msg = new ArrayList<>();
        msg.add("============ " + Util.HLT + "InfoHUD " + plu.getDescription().getVersion() + " on " + Util.serverVendor + " 1." + Util.apiVersion + Util.RES + " ============");
        if (sender instanceof Player) {
            Player p = (Player) sender;
            msg.add("");
            msg.add("Currently " + (Util.isEnabled(p) ? Util.GREN + "enabled" : Util.ERR + "disabled") + Util.RES + " for " +
                    (p.hasPermission(Util.PERM_ADMIN) ? p.getDisplayName() + Util.GREN + " (ADMIN)" : p.getDisplayName()));
            if (Util.isEnabled((Player) sender)) {
                msg.add(Util.HLT + "   coordinates: " + Util.RES + CMD_COORD.get(Util.getCoordinatesMode(p)));
                msg.add(Util.HLT + "   time: " + Util.RES + CMD_TIME.get(Util.getTimeMode(p)));
                msg.add(Util.HLT + "   darkMode: " + Util.RES + CMD_DARK.get(Util.getDarkMode(p)));
            }
        }
        msg.add("");
        msg.add(Util.GREN + "Settings");
        if (sender instanceof Player) {
            msg.add(Util.HLT + ">coordinates: " + Util.RES + "Whether or not coordinates are displayed.");
            msg.add(Util.HLT + ">time: " + Util.RES + "Format the time should be displayed in, or not at all.");
            msg.add(Util.HLT + ">darkMode: " + Util.RES + "Whether or not to display info with darker colors.");
        }
        if (sender.hasPermission(Util.PERM_ADMIN) || sender instanceof ConsoleCommandSender) {
            msg.add(Util.HLT + ">refreshRate: " + Util.RES + "Interval (in ticks) between each info update. Higher = better performance.");
            msg.add(Util.HLT + ">reload: " + Util.RES + "Reloads config.yml. " + Util.ERR + "YOU COULD LOSE SOME SETTINGS.");
            msg.add(Util.HLT + ">benchmark: " + Util.RES + "How long the last update took. A tick is 50ms.");
            msg.add(Util.HLT + ">brightBiomes: " + Util.RES + "Add/Remove biomes where dark mode turns on automatically.");
        }

        String[] msgArr = new String[msg.size()];
        sender.sendMessage(msg.toArray(msgArr));
    }

}
