package com.roverisadog.infohud;

import com.roverisadog.infohud.command.CoordMode;
import com.roverisadog.infohud.command.DarkMode;
import com.roverisadog.infohud.command.TimeMode;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Class containing all configuration options for a player.
 */
public class PlayerCfg {

    //Player management
    static Map<UUID, PlayerCfg> playerHash;
    protected UUID id;

    protected CoordMode coordMode;
    protected TimeMode timeMode;
    protected DarkMode darkMode;

    protected boolean isInBrightBiome;

    /** Checks that the player is in the list.*/
    static boolean isEnabled(Player player) {
        return playerHash.containsKey(player.getUniqueId());
    }

    static PlayerCfg getConfig(Player player) {
        return playerHash.get(player.getUniqueId());
    }

    /* ------------------------------------------ Player Management ------------------------------------------- */

    /**
     * Saves a player's UUID into player list. Assumes
     * @return Chat message signaling success or failure of operation.
     */
    static boolean savePlayer(Player player) {
        if (isEnabled(player)) {
            Util.sendMsg(player, Util.HLT + "InfoHUD was already enabled.");
            return true;
        }
        //Putting default values
        playerHash.put(player.getUniqueId(), new PlayerCfg(player.getUniqueId()));

        //Saves changes
        Util.plugin.getConfig().set(Util.PLAYER_CFG_PATH + "." + player.getUniqueId().toString(),
                playerHash.get(player.getUniqueId()).toMap());
        Util.plugin.saveConfig();

        Util.sendMsg(player, "InfoHUD is now "
                + (isEnabled(player) ? Util.GRN + "enabled" : Util.ERR + "disabled") + Util.RES + ".");
        return true;
    }

    /** Removes player UUID from player list. */
    static boolean removePlayer(Player player) {
        if (!isEnabled(player)) {
            Util.sendMsg(player, Util.HLT + "InfoHUD was already disabled.");
            return true;
        }

        playerHash.remove(player.getUniqueId());

        //Saves changes
        Util.plugin.getConfig().set(Util.PLAYER_CFG_PATH + "." + player.getUniqueId().toString(), null);
        Util.plugin.saveConfig();

        Util.sendMsg(player, "InfoHUD is now "
                + (isEnabled(player) ? Util.GRN + "enabled" : Util.ERR + "disabled") + Util.RES + ".");
        return true;
    }

    /* --------------------------------------------- Coords Mode ---------------------------------------------- */

    /** Returns coordinates display settings for player. */
    static CoordMode getCoordinatesMode(Player p) {
        return playerHash.get(p.getUniqueId()).coordMode;
    }

    /** Changes coordinates mode and returns new mode. */
    static String setCoordinatesMode(Player p, CoordMode newMode) {
        playerHash.get(p.getUniqueId()).coordMode = newMode;
        //Saves changes
        Util.plugin.getConfig().createSection(Util.PLAYER_CFG_PATH + "." + p.getUniqueId().toString(),
                playerHash.get(p.getUniqueId()).toMap());
        Util.plugin.saveConfig();
        return "Coordinates display set to: " + Util.HLT + newMode + Util.RES + ".";
    }

    /* ---------------------------------------------- Time Mode ----------------------------------------------- */

    /** Returns time display settings for player. */
    static TimeMode getTimeMode(Player p) {
        return playerHash.get(p.getUniqueId()).timeMode;
    }

    /** Changes time mode and returns new mode. */
    static String setTimeMode(Player p, TimeMode newMode) {
        if (newMode == TimeMode.VILLAGER_SCHEDULE &&  Util.apiVersion < 14) {
            return Util.ERR + "Villager schedule display is meaningless for versions before 1.14.";
        }

        playerHash.get(p.getUniqueId()).timeMode = newMode;
        //Saves changes
        Util.plugin.getConfig().createSection(Util.PLAYER_CFG_PATH + "." + p.getUniqueId().toString(),
                playerHash.get(p.getUniqueId()).toMap());
        Util.plugin.saveConfig();
        return "Time display set to: " + Util.HLT + newMode + Util.RES + ".";
    }

    /* ---------------------------------------------- Dark Mode ----------------------------------------------- */

    /** Returns dark mode settings for player. */
    static DarkMode getDarkMode(Player p) {
        return playerHash.get(p.getUniqueId()).darkMode;
    }

    /** Changes dark mode settings and returns new settings. */
    static String setDarkMode(Player p, DarkMode newMode) {
        playerHash.get(p.getUniqueId()).darkMode = newMode;

        //Saves changes
        Util.plugin.getConfig().createSection(Util.PLAYER_CFG_PATH + "." + p.getUniqueId().toString(),
                playerHash.get(p.getUniqueId()).toMap());
        Util.plugin.saveConfig();
        return "Dark mode set to: " + Util.HLT + newMode.id + Util.RES + ".";
    }

    /**
     * @return Mapping that is recognized by the bukkit yaml configuration.
     */
    protected Map<String, Object> toMap() {
        Map<String, Object> tmp = new HashMap<>();
        tmp.put(CoordMode.cfgKey, coordMode.toString());
        tmp.put(TimeMode.cfgKey, timeMode.toString());
        tmp.put(DarkMode.cfgKey, darkMode.toString());
        return tmp;
    }

    /* ---------------------------------------------- Storage ----------------------------------------------- */

    PlayerCfg(UUID id, CoordMode coordMode, TimeMode timeMode, DarkMode darkMode) {
        this.id = id;
        this.coordMode = coordMode;
        this.timeMode = timeMode;
        this.darkMode = darkMode;
    }

    protected PlayerCfg(UUID id) {
        this.id = id;
        coordMode = CoordMode.ENABLED;
        timeMode = TimeMode.CLOCK24;
        darkMode = DarkMode.AUTO;
    }

}
