
package com.roverisadog.infohud;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.block.Biome;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import javax.annotation.Nonnull;

/** Handles command completion and execution. */
public class CommandHandler implements TabExecutor {
    //Autocomplete choices
    private final static List<String> CMD = Arrays.asList("enable", "disable", "coordinates", "time", "darkMode");
    private final static List<String> CMD_ADMIN = Arrays.asList("refreshRate", "reload", "benchmark", "brightBiomes");
    private final static List<String> CMD_COORD = Arrays.asList("disabled", "enabled");
    private final static List<String> CMD_TIME = Arrays.asList("disabled", "currentTick", "clock", "villagerSchedule");
    private final static List<String> CMD_DARK = Arrays.asList("disabled", "enabled", "auto");
    private final static List<String> CMD_BIOMES = Arrays.asList("add", "remove");
    private final static List<String> BIOME_LIST = new ArrayList<>();

    public CommandHandler() {
        for (Biome b : Biome.values()) BIOME_LIST.add(b.toString());
        BIOME_LIST.add("here");
    }

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {
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
            if hasPerm(admin):
                try every admin commands
            else:
                send error missing perm
        else: #Unknown command
            send error unknown command
         */
        if (!(sender instanceof Player) && !(sender instanceof ConsoleCommandSender))
            return false;

        if (args.length == 0) {
            if (sender.hasPermission(Util.PERM_ADMIN))
                sendMsg(sender, "Usage: " + Util.HIGHLIGHT + "/" + Util.CMD_NAME + " " + Stream.concat(CMD.stream(), CMD_ADMIN.stream()).collect(Collectors.toList()) .toString());
            else
                sendMsg(sender, "Usage: " + Util.HIGHLIGHT + "/" + Util.CMD_NAME + " " + CMD.toString());
        }

        if (CMD.contains(args[0])) {

            if (sender instanceof Player) {
                Player p = (Player) sender;

                if (p.hasPermission(Util.PERM_USE)){
                    boolean enabled = Util.isEnabled(p);
                    //"enable"
                    if (args[0].equalsIgnoreCase(CMD.get(0))){
                        if (!enabled) sendMsg(p, Util.savePlayer(p));
                        else sendMsg(p, Util.HIGHLIGHT + "InfoHUD was already enabled.");
                    }
                    //"disable"
                    else if (args[0].equalsIgnoreCase(CMD.get(1))){
                        if (enabled) sendMsg(p, Util.removePlayer(p));
                        else sendMsg(p, Util.HIGHLIGHT + "InfoHUD was already disabled.");
                    }
                    //"coordinates"
                    else if (args[0].equalsIgnoreCase(CMD.get(2))){
                        if (enabled){
                            //No argument
                            if (args.length < 2) sendMsg(p, "Coordinates display is currently set to: " + Util.HIGHLIGHT + Util.COORDS_OPTIONS[Util.getCoordinatesMode(p)]);
                            //disabled : 0
                            else if (args[1].equalsIgnoreCase(CMD_COORD.get(0))) { sendMsg(p, Util.setCoordinatesMode(p, 0)); }
                            //enabled : 0
                            else if (args[1].equalsIgnoreCase(CMD_COORD.get(1))) { sendMsg(p, Util.setCoordinatesMode(p, 1)); }
                            //Unknown entry
                            else sendMsg(p, "Usage: " + Util.HIGHLIGHT + "'/" + Util.CMD_NAME + " " + CMD.get(2) + " " + CMD_COORD.toString());
                        }
                        else sendMsg(p, "Enable InfoHUD with " + Util.HIGHLIGHT + "/" + Util.CMD_NAME + " " + CMD.get(0) + Util.RES + " first.");
                    }
                    //"time"
                    else if (args[0].equalsIgnoreCase(CMD.get(3))){
                        if (enabled){
                            //No argument
                            if (args.length < 2) sendMsg(p, "Time display is currently set to: " + Util.HIGHLIGHT + Util.TIME_OPTIONS[Util.getTimeMode(p)]);
                            //disable : 0
                            else if (args[1].equalsIgnoreCase(CMD_TIME.get(0))) { sendMsg(p, Util.setTimeMode(p, 0)); }
                            //currentTick : 1
                            else if (args[1].equalsIgnoreCase(CMD_TIME.get(1))) { sendMsg(p, Util.setTimeMode(p, 1)); }
                            //clock : 2
                            else if (args[1].equalsIgnoreCase(CMD_TIME.get(2))) { sendMsg(p, Util.setTimeMode(p, 2)); }
                            //villagerSchedule : 3
                            else if (args[1].equalsIgnoreCase(CMD_TIME.get(3))) { sendMsg(p, Util.setTimeMode(p, 3)); }
                            //Default
                            else sendMsg(p, "Usage: " + Util.HIGHLIGHT + "'/" + Util.CMD_NAME + " " + CMD.get(3) + " " + CMD_TIME.toString());
                        }
                        else sendMsg(p, "Enable InfoHUD with " + Util.HIGHLIGHT + "/" + Util.CMD_NAME + " " + CMD.get(0) + Util.RES + " first.");
                    }
                    //"darkMode"
                    else if (args[0].equalsIgnoreCase(CMD.get(4))) {
                        if (enabled){
                            //No argument
                            if (args.length < 2) sendMsg(p, "Dark mode is currently set to: " + Util.HIGHLIGHT + Util.DARK_OPTIONS[Util.getDarkMode(p)]);
                            //disabled : 0
                            else if (args[1].equalsIgnoreCase(CMD_DARK.get(0))){ sendMsg(p, Util.setDarkMode(p, 0)); }
                            //enabled : 1
                            else if (args[1].equalsIgnoreCase(CMD_DARK.get(1))){ sendMsg(p, Util.setDarkMode(p, 1)); }
                            //auto : 2
                            else if (args[1].equalsIgnoreCase(CMD_DARK.get(2))){ sendMsg(p, Util.setDarkMode(p, 2)); }
                            //Default
                            else sendMsg(p, "Usage: " + Util.HIGHLIGHT + "'/" + Util.CMD_NAME + " " + CMD.get(4) + " " + CMD_DARK.toString());
                        }
                        else sendMsg(p, "Enable InfoHUD with " + Util.HIGHLIGHT + "/" + Util.CMD_NAME + " " + CMD.get(0) + Util.RES + " first.");
                    }
                }
                else sendMsg(p, Util.ERROR + "You do not have the " + Util.HIGHLIGHT + Util.PERM_USE + Util.ERROR + " permission to use this commands.");
            }
            else sendMsg(sender, Util.ERROR + "Only players may use this command.");
        }

        else if (CMD_ADMIN.contains(args[0])){
            if (sender.hasPermission(Util.PERM_ADMIN) || sender instanceof ConsoleCommandSender) {
                //"refreshRate"
                if (args[0].equalsIgnoreCase(CMD_ADMIN.get(0))) {
                    if (args.length < 2) sendMsg(sender, "Refresh rate is currently: " + Util.HIGHLIGHT + Util.getRefreshRate() + " ticks.");
                    else {
                        try { sendMsg(sender, Util.setRefreshRate(Long.parseLong(args[1]))); }
                        catch (NumberFormatException e){ sendMsg(sender, Util.ERROR + "Please enter a number between 1 and 40."); }
                    }
                }//"reload"
                else if (args[0].equalsIgnoreCase(CMD_ADMIN.get(1))) {
                    if (args.length > 1) sendMsg(sender, Util.ERROR + "This command does not take arguments.");
                    else sendMsg(sender, Util.reload() ? Util.GREN + "Reloaded successfully." : Util.ERROR + "Reload failed.");
                }//"benchmark"
                else if (args[0].equalsIgnoreCase(CMD_ADMIN.get(2))) {
                    if (args.length > 1) sendMsg(sender, Util.ERROR + "This command does not take arguments.");
                    else sendMsg(sender, Util.getBenchmark());
                }//"brightBiomes"
                else if (args[0].equalsIgnoreCase(CMD_ADMIN.get(3))) {
                    //No argument
                    if (args.length < 2) sendMsg(sender, "Usage: " + Util.HIGHLIGHT + "'/" + Util.CMD_NAME + " " + CMD_ADMIN.get(3) + " " + CMD_BIOMES.toString());
                    //"add"
                    else if (args[1].equalsIgnoreCase(CMD_BIOMES.get(0))){
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
                                sendMsg(sender, Util.ERROR + "No biome matching \"" + Util.HIGHLIGHT + args[2].toUpperCase() + Util.ERROR + "\" in version: 1." + Util.apiVersion + " found.");
                            }
                        }
                    } //"remove"
                    else if (args[1].equalsIgnoreCase(CMD_BIOMES.get(1))){
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
                                sendMsg(sender, Util.ERROR + "No biome matching \"" + Util.HIGHLIGHT + args[2].toUpperCase() + Util.ERROR + "\" in version: 1." + Util.apiVersion + " found.");
                            }
                        }
                    }
                    else sendMsg(sender, "Usage: " + Util.HIGHLIGHT + "'/" + Util.CMD_NAME + " " + CMD_ADMIN.get(3) + " " + CMD_BIOMES.toString());
                }
            }
            else sendMsg(sender, Util.ERROR + "You do not have the " + Util.HIGHLIGHT + Util.PERM_ADMIN + Util.ERROR + " permission to use this command.");
        }
        else {
            sendMsg(sender, Util.ERROR + "Unknown command.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String alias, @Nonnull String[] args) {
        boolean isPlayer = sender instanceof Player;
        boolean isConsole = sender instanceof ConsoleCommandSender;

        if (isPlayer && !sender.hasPermission(Util.PERM_USE)) return new ArrayList<>();

        if (args.length == 1){
            if (isConsole || sender.hasPermission(Util.PERM_ADMIN))
                return StringUtil.copyPartialMatches(args[0], Stream.concat(CMD.stream(), CMD_ADMIN.stream()).collect(Collectors.toList()), new ArrayList<>());
            else
                return StringUtil.copyPartialMatches(args[0], CMD, new ArrayList<>());
        }
        else if (args.length == 2){
            //"coordinates"
            if (args[0].equalsIgnoreCase(CMD.get(2))) {
                return (isPlayer && sender.hasPermission(Util.PERM_USE)) ? StringUtil.copyPartialMatches(args[1], CMD_COORD, new ArrayList<>()) : new ArrayList<>();
            }
            //"time"
            else if (args[0].equalsIgnoreCase(CMD.get(3))){
                return (isPlayer && sender.hasPermission(Util.PERM_USE)) ? StringUtil.copyPartialMatches(args[1], CMD_TIME, new ArrayList<>()) : new ArrayList<>();
            }
            //"darkMode"
            else if (args[0].equalsIgnoreCase(CMD.get(4))){
                return (isPlayer && sender.hasPermission(Util.PERM_USE)) ? StringUtil.copyPartialMatches(args[1], CMD_DARK, new ArrayList<>()) : new ArrayList<>();
            }
            //"refreshRate"
            else if (args[0].equalsIgnoreCase(CMD_ADMIN.get(0))){
                return (sender.hasPermission(Util.PERM_ADMIN)) ? Collections.singletonList("5") : new ArrayList<>();
            }
            //"brightBiomes"
            else if (args[0].equalsIgnoreCase(CMD_ADMIN.get(3))){
                return (sender.hasPermission(Util.PERM_ADMIN)) ? StringUtil.copyPartialMatches(args[1], CMD_BIOMES, new ArrayList<>()) : new ArrayList<>();
            }
        }
        else if (args.length == 3) {
            if (args[0].equalsIgnoreCase(CMD_ADMIN.get(3))) {
                //"add"
                if (args[1].equalsIgnoreCase(CMD_BIOMES.get(0))) {
                    return BIOME_LIST;
                }
                //"remove"
                else if (args[1].equalsIgnoreCase(CMD_BIOMES.get(1))) {
                    ArrayList<String> temp = Util.getBrightBiomesList();
                    temp.add("currentBiome");
                    return temp;
                }
            }
        }
        return new ArrayList<>();
    }

    /** Send chat message to command sender. */
    static void sendMsg(CommandSender sender, String msg) {
        sender.sendMessage(Util.SIGNATURE + "[InfoHUD] " + Util.RES + msg);
    }

}
