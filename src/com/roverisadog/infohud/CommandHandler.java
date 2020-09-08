
package com.roverisadog.infohud;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import javax.annotation.Nonnull;

public class CommandHandler implements TabExecutor {
    //Autocomplete choices
    private final static List<String> CMD = Arrays.asList("enable", "disable", "coordinates", "time", "darkMode", "refreshRate");
    private final static List<String> CMD_COORD = Arrays.asList("disabled", "enabled");
    private final static List<String> CMD_TIME = Arrays.asList("disabled", "currentTick", "clock", "villagerSchedule");
    private final static List<String> CMD_DARK = Arrays.asList("disabled", "enabled", "auto");

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {
        if (!(sender instanceof Player)) return false;

        Player p = (Player)sender;

        boolean enabled = Util.isOnList(p);

        //Check permissions
        if (!p.hasPermission(Util.PERM_USE)) {
            sendMsg(p, "You do not have the " + Util.PERM_USE + " permission to use this command.");
            return true;
        }

        if (args.length == 0) sendMsg(p, "Usage: " + ChatColor.YELLOW + "/" + Util.CMD_NAME + " " + CMD.toString());

        //Try "enable"
        else if (args[0].equalsIgnoreCase(CMD.get(0))){
            if (!enabled) sendMsg(p, Util.savePlayer(p));
            else sendMsg(p, "InfoHUD was already enabled.");
        }

        //Try "disable"
        else if (args[0].equalsIgnoreCase(CMD.get(1))){
            if (enabled) sendMsg(p, Util.removePlayer(p));
            else sendMsg(p, "InfoHUD was already disabled.");
        }

        //Try changing "coordinates" mode
        else if (args[0].equalsIgnoreCase(CMD.get(2))){
            if (enabled){
                if (args.length < 2) sendMsg(p, "Coordinates display is currently set to: " + Util.COORDS_OPTIONS[Util.getCFG(p)[0]]);
                //disabled : 0
                else if (args[1].equalsIgnoreCase(CMD_COORD.get(0))) { sendMsg(p, Util.setCoordMode(p, 0)); }
                //enabled : 0
                else if (args[1].equalsIgnoreCase(CMD_COORD.get(1))) { sendMsg(p, Util.setCoordMode(p, 1)); }
                //Unknown entry
                else sendMsg(p, "Usage: " + ChatColor.YELLOW + "'/" + Util.CMD_NAME + " " + CMD.get(2) + " " + CMD_COORD.toString());
            }
            else sendMsg(p, "Enable InfoHUD with " + ChatColor.YELLOW + "'/" + Util.CMD_NAME + " " + CMD.get(0) + "' first.");
        }

        //Try changing "time" display format
        else if (args[0].equalsIgnoreCase(CMD.get(3))){
            if (enabled){
                if (args.length < 2) sendMsg(p, "Time display is currently set to: " + Util.TIME_OPTIONS[Util.getCFG(p)[1]]);
                //disable : 0
                else if (args[1].equalsIgnoreCase(CMD_TIME.get(0))) { sendMsg(p, Util.setTimeMode(p, 0)); }
                //currentTick : 1
                else if (args[1].equalsIgnoreCase(CMD_TIME.get(1))) { sendMsg(p, Util.setTimeMode(p, 1)); }
                //clock : 2
                else if (args[1].equalsIgnoreCase(CMD_TIME.get(2))) { sendMsg(p, Util.setTimeMode(p, 2)); }
                //villagerSchedule : 3
                else if (args[1].equalsIgnoreCase(CMD_TIME.get(3))) { sendMsg(p, Util.setTimeMode(p, 3)); }
                //Default
                else sendMsg(p, "Usage: " + ChatColor.YELLOW + "'/" + Util.CMD_NAME + " " + CMD.get(3) + " " + CMD_TIME.toString());
            }
            else sendMsg(p, "Enable InfoHUD with " + ChatColor.YELLOW + "'/" + Util.CMD_NAME + " " + CMD.get(0) + "' first.");
        }

        //Try changing "darkMode" settings
        else if (args[0].equalsIgnoreCase(CMD.get(4))) {
            if (enabled){
                if (args.length < 2) sendMsg(p, "Dark mode is currently set to: " + Util.DARK_OPTIONS[Util.getCFG(p)[2]]);
                //disabled : 0
                else if (args[1].equalsIgnoreCase(CMD_DARK.get(0))){ sendMsg(p, Util.setDarkMode(p, 0)); }
                //enabled : 1
                else if (args[1].equalsIgnoreCase(CMD_DARK.get(1))){ sendMsg(p, Util.setDarkMode(p, 1)); }
                //auto : 2
                else if (args[1].equalsIgnoreCase(CMD_DARK.get(2))){ sendMsg(p, Util.setDarkMode(p, 2)); }
                //Default
                else sendMsg(p, "Usage: " + ChatColor.YELLOW + "'/" + Util.CMD_NAME + " " + CMD.get(4) + " " + CMD_DARK.toString());
            }
            else sendMsg(p, "Enable InfoHUD with " + ChatColor.YELLOW + "'/" + Util.CMD_NAME + " " + CMD.get(0) + "' first.");
        }
        //TODO finish
        //Try changing "refreshRate"
        else if (args[0].equalsIgnoreCase(CMD.get(5))) {

            if (!p.hasPermission(Util.PERM_ADMIN)) {
                sendMsg(p, "You do not have the " + Util.PERM_ADMIN + " permission to use this command.");
            }
            else {
                sendMsg(p, "Command unfinished. Modify field " + Util.YEL + "'refreshRate'" + Util.RES + " in config.yml and restart.");
                /*
                if (args.length < 2) sendMsg(p, "Refresh rate is currently: " + Util.getRefreshRate());
                else {
                    try {
                        sendMsg(p, Util.changeUpdateRate(Long.parseLong(args[1])));
                    } catch (NumberFormatException e){
                        sendMsg(p, "Please enter a number.");
                    }
                }
                */
            }
        }
        //Default
        else {
            sendMsg(p, "Did not recognize command.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String alias, @Nonnull String[] args) {

        if (args.length == 1){
            return StringUtil.copyPartialMatches(args[0], CMD, new ArrayList<>());
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

            else return null;
        }
        return null;
    }

    /** Send chat message to player. */
    static void sendMsg(CommandSender player, String msg) {
        player.sendMessage(Util.BLU + "[InfoHUD] " + Util.RES + msg);
    }

}
