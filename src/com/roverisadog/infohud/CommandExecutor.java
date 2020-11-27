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
    private static final List<String> CMD_NORMAL =
            Arrays.asList("enable", "disable", CoordMode.cmdName, TimeMode.cmdName, DarkMode.cmdName, "help");
    private static final List<String> CMD_ADMIN =
            Arrays.asList("refreshRate", "reload", "benchmark", "brightBiomes");
    private static final List<String> ALL_CMD = Stream.concat(CMD_NORMAL.stream(), CMD_ADMIN.stream())
            .collect(Collectors.toList());

    private static final List<String> CMD_BIOMES =
            Arrays.asList("add", "remove");
    private static final List<String> BIOME_LIST = new ArrayList<>();

    /** Instance of the plugin. */
    private final Plugin plugin;

    static {
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

        // [/infohud]
        else if (args.length == 0) {
            if (sender.hasPermission(Util.PERM_ADMIN)) {
                sendMsg(sender, "Usage: " + Util.HLT + "/" + Util.CMD_NAME + " " +
                        Stream.concat(CMD_NORMAL.stream(), CMD_ADMIN.stream())
                                .collect(Collectors.toList())
                                .toString());
            }
            else {
                sendMsg(sender, "Usage: " + Util.HLT + "/" + Util.CMD_NAME + " " + CMD_NORMAL.toString());
            }
            return true;
        }

        //Is normal command
        else if (CMD_NORMAL.contains(args[0])) {

            // [/infohud help]
            if (args[0].equalsIgnoreCase(CMD_NORMAL.get(5))) {
                buildHelpMenu(sender);
                return true;
            }

            //Not player
            if (!(sender instanceof Player)) {
                sendMsg(sender, Util.ERR + "Only players may use this command.");
                return true;
            }

            Player p = (Player) sender;

            //Doesn't have infohud.use permission.
            if (!p.hasPermission(Util.PERM_USE)) {
                sendMsg(p, Util.ERR + "You do not have the " + Util.HLT
                        + Util.PERM_USE + Util.ERR + " permission to use this commands.");
                return true;
            }

            boolean enabled = Util.isEnabled(p);

            // [/infohud enable]
            if (args[0].equalsIgnoreCase(CMD_NORMAL.get(0))) {
                if (!enabled) {
                    sendMsg(p, Util.savePlayer(p));
                }
                else {
                    sendMsg(p, Util.HLT + "InfoHUD was already enabled.");
                }
                return true;
            }

            // [/infohud disable]
            else if (args[0].equalsIgnoreCase(CMD_NORMAL.get(1))) {
                if (enabled) {
                    sendMsg(p, Util.removePlayer(p));
                }
                else {
                    sendMsg(p, Util.HLT + "InfoHUD was already disabled.");
                }
                return true;
            }

            else if (!enabled) {
                sendMsg(p, "Enable InfoHUD with " + Util.HLT + "/" + Util.CMD_NAME + " " + CMD_NORMAL.get(0) + Util.RES + " first.");
                return true;
            }

            // [/infohud coordinates]
            else if (args[0].equalsIgnoreCase(CMD_NORMAL.get(2))) {
                return setCoordinates(p, args, 1);
            }

            // [/infohud time]
            else if (args[0].equalsIgnoreCase(CMD_NORMAL.get(3))) {
                return setTime(p, args, 1);
            }

            // [/infohud darkMode]
            else if (args[0].equalsIgnoreCase(CMD_NORMAL.get(4))) {
                return setDarkMode(p, args, 1);
            }
            return true;
        }

        //Admin command
        else if (CMD_ADMIN.contains(args[0])) {

            //Doesn't have the infohud.admin permission and isn't console
            if (!sender.hasPermission(Util.PERM_ADMIN) && !(sender instanceof ConsoleCommandSender)) {
                sendMsg(sender, Util.ERR + "You do not have the " + Util.HLT
                        + Util.PERM_ADMIN + Util.ERR + " permission to use this command.");
                return true;
            }

            // [/infohud refreshRate]
            if (args[0].equalsIgnoreCase(CMD_ADMIN.get(0))) {
                //No number
                if (args.length < 2) {
                    sendMsg(sender, "Refresh rate is currently: " + Util.HLT + Util.getRefreshRate() + " ticks.");
                }
                //Gives number
                else {
                    try {
                        sendMsg(sender, Util.setRefreshRate(Long.parseLong(args[1])));
                    } catch (NumberFormatException e) {
                        sendMsg(sender, Util.ERR + "Please enter a positive integer between 1 and 40.");
                    }
                }
            }

            // [/infohud reload]
            else if (args[0].equalsIgnoreCase(CMD_ADMIN.get(1))) {
                if (args.length > 1) {
                    sendMsg(sender, Util.ERR + "This command does not take arguments.");
                }
                else {
                    sendMsg(sender, Util.reload() ? Util.GRN + "Reloaded successfully." : Util.ERR + "Reload failed.");
                }
            }

            // [/infohud benchmark]
            else if (args[0].equalsIgnoreCase(CMD_ADMIN.get(2))) {
                if (args.length > 1) {
                    sendMsg(sender, Util.ERR + "This command does not take arguments.");
                }
                else {
                    sendMsg(sender, Util.getBenchmark());
                }
            }

            // [/infohud brightBiomes]
            else if (args[0].equalsIgnoreCase(CMD_ADMIN.get(3))) {
                return setBiomes(sender, args, 1);
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

        //Does not have infohud.use permission
        if (isPlayer && !sender.hasPermission(Util.PERM_USE)) {
            return new ArrayList<>();
        }

        //1st keyword [/infohud X]
        else if (args.length == 1) {
            if (isConsole || sender.hasPermission(Util.PERM_ADMIN)) {
                return StringUtil.copyPartialMatches(args[0], ALL_CMD, new ArrayList<>());
            }
            else {
                return StringUtil.copyPartialMatches(args[0], CMD_NORMAL, new ArrayList<>());
            }
        }

        //2nd keyword [/infohud CMD ---]
        else if (args.length == 2) {
            //"coordinates"
            if (CoordMode.cmdName.equalsIgnoreCase(args[0])) {
                return (isPlayer)
                        ? StringUtil.copyPartialMatches(args[1], CoordMode.stringList, new ArrayList<>())
                        : new ArrayList<>(); //Void if not player.
            }
            //"time"
            else if (TimeMode.cmdName.equalsIgnoreCase(args[0])) {
                return (isPlayer)
                        ? StringUtil.copyPartialMatches(args[1], TimeMode.stringList, new ArrayList<>())
                        : new ArrayList<>(); //Void if not player.
            }
            //"darkMode"
            else if (DarkMode.cmdName.equalsIgnoreCase(args[0])) {
                return (isPlayer)
                        ? StringUtil.copyPartialMatches(args[1], DarkMode.stringList, new ArrayList<>())
                        : new ArrayList<>(); //Void if not player.
            }
            //"refreshRate"
            else if (args[0].equalsIgnoreCase(CMD_ADMIN.get(0))) {
                return (sender.hasPermission(Util.PERM_ADMIN)) ? Collections.singletonList("5") : new ArrayList<>();
            }
            //"brightBiomes"
            else if (args[0].equalsIgnoreCase(CMD_ADMIN.get(3))) {
                return (sender.hasPermission(Util.PERM_ADMIN))
                        ? StringUtil.copyPartialMatches(args[1], CMD_BIOMES, new ArrayList<>())
                        : new ArrayList<>();
            }
        }
        //3rd keyword [/infohud CMD SETTING ---]
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
        //Unrecognized
        return new ArrayList<>();
    }

    /** Send chat message to command sender. */
    private void sendMsg(CommandSender sender, String msg) {
        sender.sendMessage(Util.SIGNATURE + "[InfoHUD] " + Util.RES + msg);
    }

    private void buildHelpMenu(CommandSender sender) {
        List<String> msg = new ArrayList<>();
        msg.add("============ " + Util.HLT + "InfoHUD " + plugin.getDescription().getVersion() + " on "
                + Util.serverVendor + " 1." + Util.apiVersion + Util.RES + " ============");

        //Display current player's settings.
        if (sender instanceof Player) {
            Player p = (Player) sender;
            msg.add("");
            msg.add("Currently "
                    + (Util.isEnabled(p) ? Util.GRN + "enabled" : Util.ERR + "disabled")
                    + Util.RES + " for "
                    + (p.hasPermission(Util.PERM_ADMIN) ? p.getDisplayName() + Util.GRN + " (ADMIN)"
                    : p.getDisplayName()));

            if (Util.isEnabled((Player) sender)) {
                msg.add(Util.HLT + "   coordinates: " + Util.RES + CoordMode.get(Util.getCoordinatesMode(p)));
                msg.add(Util.HLT + "   time: " + Util.RES + TimeMode.get(Util.getTimeMode(p)));
                msg.add(Util.HLT + "   darkMode: " + Util.RES + DarkMode.get(Util.getDarkMode(p)));
            }
        }

        //Display normal user commands.
        msg.add("");
        msg.add(Util.GRN + "Settings");
        if (sender instanceof Player) {
            msg.add(Util.HLT + ">coordinates: " + Util.RES + "Whether or not coordinates are displayed.");
            msg.add(Util.HLT + ">time: " + Util.RES + "Format the time should be displayed in, or not at all.");
            msg.add(Util.HLT + ">darkMode: " + Util.RES + "Whether or not to display info with darker colors.");
        }

        //Display admin commands.
        if (sender.hasPermission(Util.PERM_ADMIN) || sender instanceof ConsoleCommandSender) {
            msg.add(Util.HLT + ">refreshRate: "
                    + Util.RES + "Interval (in ticks) between each info update. Higher = better performance.");
            msg.add(Util.HLT + ">reload: "
                    + Util.RES + "Reloads config.yml. " + Util.ERR + "YOU COULD LOSE SOME SETTINGS.");
            msg.add(Util.HLT + ">benchmark: "
                    + Util.RES + "How long the last update took. A tick is 50ms.");
            msg.add(Util.HLT + ">brightBiomes: "
                    + Util.RES + "Add/Remove biomes where dark mode turns on automatically.");
        }

        String[] msgArr = new String[msg.size()];
        sender.sendMessage(msg.toArray(msgArr));
    }

    private boolean setCoordinates(Player p, String[] args, int argsStart) {
        //No argument
        if (args.length < argsStart + 1) {
            sendMsg(p, "Coordinates display is currently set to: " + Util.HLT + Util.COORDS_OPTIONS[Util.getCoordinatesMode(p)]);
            return true;
        }

        //Cycle through all coordinate modes
        for (CoordMode cm : CoordMode.values()) {
            if (cm.name.equalsIgnoreCase(args[argsStart])) {
                sendMsg(p, Util.setCoordinatesMode(p, cm));
                return true;
            }
        }

        //Unrecognized argument
        sendMsg(p, "Usage: " + Util.HLT + "'/" +
                Util.CMD_NAME + " " + CMD_NORMAL.get(2) + " " + Arrays.toString(CoordMode.values()));
        return true;
    }

    private boolean setTime(Player p, String[] args, int argsStart) {
        //No argument
        if (args.length < argsStart + 1) {
            sendMsg(p, "Time display is currently set to: " + Util.HLT + Util.TIME_OPTIONS[Util.getTimeMode(p)]);
            return true;
        }

        //Cycle through all time modes
        for (TimeMode tm : TimeMode.values()) {
            if (tm.name.equalsIgnoreCase(args[argsStart])) {
                sendMsg(p, Util.setTimeMode(p, tm));
                return true;
            }
        }

        //Unrecognized argument
        sendMsg(p, "Usage: " + Util.HLT + "'/" +
                Util.CMD_NAME + " " + CMD_NORMAL.get(3) + " " + Arrays.toString(TimeMode.values()));
        return true;
    }

    private boolean setDarkMode(Player p, String[] args, int argsStart) {
        //No argument
        if (args.length < argsStart + 1) {
            sendMsg(p, "Dark mode is currently set to: " + Util.HLT + Util.DARK_OPTIONS[Util.getDarkMode(p)]);
            return true;
        }

        //Cycle through all time modes
        for (DarkMode dm : DarkMode.values()) {
            if (dm.name.equalsIgnoreCase(args[argsStart])) {
                sendMsg(p, Util.setDarkMode(p, dm));
                return true;
            }
        }

        //Unrecognized argument
        sendMsg(p, "Usage: " + Util.HLT + "'/" +
                Util.CMD_NAME + " " + CMD_NORMAL.get(4) + " " + Arrays.toString(DarkMode.values()));
        return true;
    }

    private boolean setBiomes(CommandSender sender, String[] args, int argsStart) {
        //No argument
        if (args.length < argsStart + 1) {
            sendMsg(sender, "Usage: " + Util.HLT + "/" + Util.CMD_NAME + " "
                    + CMD_ADMIN.get(3) + " " + CMD_BIOMES.toString());
        }
        // [/infohud biome add]
        else if (args[argsStart].equalsIgnoreCase(CMD_BIOMES.get(0))) {
            if (args.length < 3) {
                sendMsg(sender, "Enter a biome name.");
            }
            else {
                try {
                    //currentBiome
                    if ((sender instanceof Player) && args[argsStart + 1].equalsIgnoreCase("here")) {
                        sendMsg(sender, Util.addBrightBiome(((Player) sender).getLocation().getBlock().getBiome()));
                    }
                    //ANY_BIOME
                    else {
                        Biome b = Biome.valueOf(args[argsStart + 1].toUpperCase());
                        sendMsg(sender, Util.addBrightBiome(b));
                    }
                } catch (Exception e) {
                    sendMsg(sender, Util.ERR
                            + "No biome matching \"" + Util.HLT + args[argsStart + 1].toUpperCase()
                            + Util.ERR + "\" found in version: 1." + Util.apiVersion + " .");
                }
            }
        }
        // [/infohud biome remove]
        else if (args[argsStart].equalsIgnoreCase(CMD_BIOMES.get(1))) {
            //No argument
            if (args.length < argsStart + 1) {
                sendMsg(sender, "Enter a biome name.");
            }
            else {
                try {
                    //currentBiome
                    if ((sender instanceof Player) && args[argsStart + 1].equalsIgnoreCase("here")) {
                        sendMsg(sender, Util.removeBrightBiome(((Player) sender).getLocation().getBlock().getBiome()));
                    }
                    //ANY_BIOME
                    else {
                        Biome b = Biome.valueOf(args[2].toUpperCase());
                        sendMsg(sender, Util.removeBrightBiome(b));
                    }
                } catch (Exception e) {
                    sendMsg(sender, Util.ERR
                            + "No biome matching \"" + Util.HLT + args[argsStart + 1].toUpperCase()
                            + Util.ERR + "\" found in version: 1." + Util.apiVersion + " .");
                }
            }
        }
        else {
            sendMsg(sender, "Usage: " + Util.HLT + "'/" + Util.CMD_NAME + " "
                    + CMD_ADMIN.get(3) + " " + CMD_BIOMES.toString());
        }
        return true;
    }

}
