
package com.roverisadog.infohud;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import javax.annotation.Nonnull;

/** Handles command completion and execution. */
public class CommandHandler implements TabExecutor {
    //Autocomplete choices
    private final static List<String> CMD = Arrays.asList("enable", "disable", "coordinates", "time", "darkMode");
    private final static List<String> CMD_ADMIN = Arrays.asList("refreshRate", "reload", "benchmark");
    private final static List<String> CMD_COORD = Arrays.asList("disabled", "enabled");
    private final static List<String> CMD_TIME = Arrays.asList("disabled", "currentTick", "clock", "villagerSchedule");
    private final static List<String> CMD_DARK = Arrays.asList("disabled", "enabled", "auto");

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {
        if (!(sender instanceof Player)) return false;

        Player p = (Player)sender;

        boolean enabled = Util.isEnabled(p);

        //Check permissions
        if (!p.hasPermission(Util.PERM_USE)) {
            sendMsg(p, Util.ERROR + "You do not have the " + Util.HIGHLIGHT + Util.PERM_USE + Util.ERROR + " permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            if (p.hasPermission(Util.PERM_ADMIN))
                sendMsg(p, "Usage: " + Util.HIGHLIGHT + "/" + Util.CMD_NAME + " " + Stream.concat(CMD.stream(), CMD_ADMIN.stream()).collect(Collectors.toList()) .toString());
            else
                sendMsg(p, "Usage: " + Util.HIGHLIGHT + "/" + Util.CMD_NAME + " " + CMD.toString());
        }

        //"enable"
        else if (args[0].equalsIgnoreCase(CMD.get(0))){
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
                if (args.length < 2) sendMsg(p, "Coordinates display is currently set to: " + Util.HIGHLIGHT + Util.COORDS_OPTIONS[Util.getCoordinatesMode(p)]);
                //disabled : 0
                else if (args[1].equalsIgnoreCase(CMD_COORD.get(0))) { sendMsg(p, Util.setCoordMode(p, 0)); }
                //enabled : 0
                else if (args[1].equalsIgnoreCase(CMD_COORD.get(1))) { sendMsg(p, Util.setCoordMode(p, 1)); }
                //Unknown entry
                else sendMsg(p, "Usage: " + Util.HIGHLIGHT + "'/" + Util.CMD_NAME + " " + CMD.get(2) + " " + CMD_COORD.toString());
            }
            else sendMsg(p, "Enable InfoHUD with " + Util.HIGHLIGHT + "/" + Util.CMD_NAME + " " + CMD.get(0) + Util.RES + " first.");
        }

        //"time"
        else if (args[0].equalsIgnoreCase(CMD.get(3))){
            if (enabled){
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
        //"refreshRate"
        else if (args[0].equalsIgnoreCase(CMD_ADMIN.get(0))) {
            if (!p.hasPermission(Util.PERM_ADMIN)) sendMsg(p, Util.ERROR + "You do not have the " + Util.HIGHLIGHT + Util.PERM_ADMIN + Util.ERROR + " permission to use this command.");
            else {
                if (args.length < 2) sendMsg(p, "Refresh rate is currently: " + Util.HIGHLIGHT + Util.getRefreshRate() + "ticks.");
                else {
                    try { sendMsg(p, Util.setRefreshRate(Long.parseLong(args[1]))); }
                    catch (NumberFormatException e){ sendMsg(p, Util.ERROR + "Please enter a number between 1 and 40."); }
                }
            }
        }//"reload"
        else if (args[0].equalsIgnoreCase(CMD_ADMIN.get(1))) {
            if (!p.hasPermission(Util.PERM_ADMIN)) sendMsg(p, Util.ERROR + "You do not have the " + Util.HIGHLIGHT + Util.PERM_ADMIN + Util.ERROR + " permission to use this command.");
            else {
                if (args.length > 1) sendMsg(p, Util.ERROR + "This command does not take arguments.");
                else sendMsg(p, Util.reload() ? Util.GREN + "Reloaded successfully." : Util.ERROR + "Reload failed.");
            }
        }//"benchmark"
        else if (args[0].equalsIgnoreCase(CMD_ADMIN.get(2))) {
            if (!p.hasPermission(Util.PERM_ADMIN)) sendMsg(p, Util.ERROR + "You do not have the " + Util.HIGHLIGHT + Util.PERM_ADMIN + Util.ERROR + " permission to use this command.");
            else {
                if (args.length > 1) sendMsg(p, Util.ERROR + "This command does not take arguments.");
                else sendMsg(p, Util.getBenchmark());
            }
        }

        //Default
        else {
            sendMsg(p, Util.ERROR + "Unknown command.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String alias, @Nonnull String[] args) {

        if (!sender.hasPermission(Util.PERM_USE)) return new ArrayList<>();

        if (args.length == 1){
            if (sender.hasPermission(Util.PERM_ADMIN)) return StringUtil.copyPartialMatches(args[0], Stream.concat(CMD.stream(), CMD_ADMIN.stream()).collect(Collectors.toList()), new ArrayList<>());
            else return StringUtil.copyPartialMatches(args[0], CMD, new ArrayList<>());
        }
        else if (args.length == 2){
            //"coordinates"
            if (args[0].equalsIgnoreCase(CMD.get(2))) {
                return StringUtil.copyPartialMatches(args[1], CMD_COORD, new ArrayList<>());
            }
            //"time"
            else if (args[0].equalsIgnoreCase(CMD.get(3))){
                return StringUtil.copyPartialMatches(args[1], CMD_TIME, new ArrayList<>());
            }
            //"darkMode"
            else if (args[0].equalsIgnoreCase(CMD.get(4))){
                return StringUtil.copyPartialMatches(args[1], CMD_DARK, new ArrayList<>());
            }
            //"refreshRate"
            else if (args[0].equalsIgnoreCase(CMD_ADMIN.get(0))){
                return Collections.singletonList("5");
            }
        }
        return new ArrayList<>();
    }

    /** Send chat message to player. */
    static void sendMsg(CommandSender player, String msg) {
        player.sendMessage(Util.SIGNATURE + "[InfoHUD] " + Util.RES + msg);
    }

}
