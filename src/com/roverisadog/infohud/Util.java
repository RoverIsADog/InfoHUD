
package com.roverisadog.infohud;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/** Helper class. */
public class Util {
    //Main plugin thread
    static BukkitTask task;

    //Main Plugin class instance
    private static InfoHUD plugin;

    //Player management
    private static HashMap<UUID, int[]> playerHash;
    static final int[] DEFAULT_CFG = new int[]{1, 2, 2}; //[coordsMode, timeMode, darkModeCFG]

    static HashMap<Biome, Object> brightBiomes;

    //Default values
    static final String CMD_NAME = "infohud";
    static final String[] COORDS_OPTIONS = new String[]{"disabled", "enabled"};
    static final String[] TIME_OPTIONS = new String[]{"disabled", "ticks", "24 hours clock", "villager schedule"};
    static final String[] DARK_OPTIONS = new String[]{"disabled", "enabled", "auto"};

    //Shortcuts
    static final String PERM_USE = "infohud.use";
    static final String PERM_ADMIN = "infohud.admin";

    static final String HIGHLIGHT = ChatColor.YELLOW.toString();
    static final String SIGNATURE = ChatColor.BLUE.toString();
    static final String ERROR = ChatColor.RED.toString();

    static final String RES = ChatColor.RESET.toString();
    static final String WHI = ChatColor.WHITE.toString();
    static final String GLD = ChatColor.GOLD.toString();
    static final String DAQA = ChatColor.DARK_AQUA.toString();
    static final String AQA = ChatColor.AQUA.toString();
    static final String DBLU = ChatColor.DARK_BLUE.toString();

    private static long refreshRate;

    /** Loads disk contents of config.yml into memory. Returns false if an exception is found. */
    public static boolean loadConfig(Plugin plu) {
        try {
            Util.plugin = (InfoHUD) plu;

            Util.refreshRate = plu.getConfig().getLong("refreshRate");

            //[coordinatesMode, timeMode, darkMode]
            Util.playerHash = new HashMap<>();
            for (String player : Objects.requireNonNull(plu.getConfig().getConfigurationSection("playerConfig")).getKeys(false)) {
                Util.playerHash.put(UUID.fromString(player), plu.getConfig().getIntegerList("playerConfig." + player).stream().mapToInt(i->i).toArray());
            }

            Util.brightBiomes = new HashMap<>();
            for (String cur : plu.getConfig().getStringList("brightBiomes")){
                Util.brightBiomes.put(Biome.valueOf(cur), null);
            }
            return true;
        } catch (Exception e){
            return false;
        }

    }

    /** Checks that the player is in the list.*/
    static boolean isOnList(Player player) {
        return playerHash.containsKey(player.getUniqueId());
    }

    /** Saves player UUID into player list. */
    static String savePlayer(Player player) {
        //Save into hashmap and string list
        playerHash.put(player.getUniqueId(), DEFAULT_CFG.clone());

        //Saves changes
        plugin.getConfig().set("playerConfig." + player.getUniqueId().toString(), playerHash.get(player.getUniqueId()));
        plugin.saveConfig();

        return "InfoHUD is now " + HIGHLIGHT + (Util.isOnList(player) ? "enabled" : "disabled") + RES + ".";
    }

    /** Removes player UUID from player list. */
    static String removePlayer(Player player) {
        //Remove from hashmap and string list
        playerHash.remove(player.getUniqueId());

        //Saves changes
        plugin.getConfig().set("playerConfig." + player.getUniqueId().toString(), null);
        plugin.saveConfig();

        return "InfoHUD is now " + HIGHLIGHT + (Util.isOnList(player) ? "enabled" : "disabled") + RES + ".";
    }

    /** Changes coordinates mode and returns new mode. */
    static String setCoordMode(Player p, int newMode){
        int[] cfg = playerHash.get(p.getUniqueId());
        cfg[0] = newMode;
        //Saves changes
        plugin.getConfig().set("playerConfig." + p.getUniqueId().toString(), cfg);
        plugin.saveConfig();
        return "Coordinates display set to: " + HIGHLIGHT + COORDS_OPTIONS[newMode] + RES;
    }

    /** Changes time mode and returns new mode. */
    static String setTimeMode(Player p, int newMode){
        int[] cfg = playerHash.get(p.getUniqueId());
        cfg[1] = newMode;
        //Saves changes
        plugin.getConfig().set("playerConfig." + p.getUniqueId().toString(), cfg);
        plugin.saveConfig();
        return "Time display set to: " + HIGHLIGHT + TIME_OPTIONS[newMode] + RES;
    }

    /** Changes dark mode settings and returns new settings. */
    static String setDarkMode(Player p, int newMode){
        int[] cfg = playerHash.get(p.getUniqueId());
        cfg[2] = newMode;
        //Saves changes
        plugin.getConfig().set("playerConfig." + p.getUniqueId().toString(), cfg);
        plugin.saveConfig();
        return "Dark mode set to: " + HIGHLIGHT + DARK_OPTIONS[newMode] + RES;
    }

    /** Returns string of player position. */
    static String getCoordinates(Player p){
        //Location
        int x = p.getLocation().getBlockX();
        int y = p.getLocation().getBlockY();
        int z = p.getLocation().getBlockZ();

        return x + " " + y + " " + z;
    }

    /** Converts minecraft internal clock to HH:mm string. */
    static String getTime24(long time){
        //MC day starts at 6:00: https://minecraft.gamepedia.com/Daylight_cycle
        String timeH = Long.toString((time/1000L + 6L) % 24L);
        String timeM = String.format("%02d", time % 1000L * 60L / 1000L);
        return timeH + ":" + timeM;
    }

    /** Returns current villager schedule and time before change. */
    static String getVillagerTimeLeft(long time, String col1, String col2) {
        //Sleeping 12000 - 0
        if (time > 12000L){
            long remaining = 12000L - time + 12000L;
            return col1 + "Sleep: " + col2 + remaining/1200L + ":" + String.format("%02d", remaining % 1200L / 20L);
        }
        //Wandering 11000 - 12000
        else if (time > 11000L){
            long remaining = 1000L - time + 11000L;
            return col1 + "Wander: " + col2 + remaining/1200L + ":" + String.format("%02d", remaining % 1200L / 20L);
        }
        //Gathering 9000 - 11000
        else if (time > 9000L){
            long remaining = 2000L - time + 9000L;
            return col1 + "Gather: " + col2 + remaining/1200L + ":" + String.format("%02d", remaining % 1200L / 20L);
        }
        //Working 2000 - 9000
        else if (time > 2000L){
            long remaining = 7000L - time + 2000L;
            return col1 + "Work: " + col2 + remaining/1200L + ":" + String.format("%02d", remaining % 1200L / 20L);
        }
        //Wandering 0 - 2000
        else{
            long remaining = 2000L - time;
            return col1 + "Wander: " + col2 + remaining/1200L + ":" + String.format("%02d", remaining % 1200L / 20L);
        }
    }

    /** Calculates direction the player is facing and returns corresponding string. */
    static String getPlayerDirection(Player player) {
        //-180: Leaning left | +180: Leaning right
        float yaw = player.getLocation().getYaw();

        //Bring to 360 degrees (Clockwise from -X axis)
        if (yaw < 0.0F) {
            yaw += 360.0F;
        }

        //Separate into 8 sectors (Arc: 45deg), offset by 1/2 sector (Arc: 22.5deg)
        int sector = (int)((yaw + 22.5F) / 45F);
        switch (sector){
            case 1:
                return "SW";
            case 2:
                return "W [-X]";
            case 3:
                return "NW";
            case 4:
                return "N [-Z]";
            case 5:
                return "NE";
            case 6:
                return "E [+X]";
            case 7:
                return "SE";
            case 0:
            default:
                //Example: (359 + 22.5)/45
                return "S [+Z]";
        }
    }

    /** How many tick between each refresh. Values higher than 20 may lead to actionbar text fading. */
    static long getRefreshRate() {
        return refreshRate;
    }

    /** Reloads content of config.yml */
    static boolean reload(){
        return loadConfig(plugin);
    }

    /** Change how many ticks between each refresh. */
    static String changeRefreshRate(long newRate) {
        if (newRate == 0 || newRate > 40) return ERROR + "Number must be between 1 and 40 ticks";
        refreshRate = newRate;
        //Saves value for next time
        plugin.getConfig().set("refreshRate", refreshRate);
        plugin.saveConfig();
        //Stop task and restart with new refresh rate
        task.cancel();
        task = plugin.start(Util.plugin);
        return "Refresh rate set to " + HIGHLIGHT + newRate;
    }

    /** Returns int array of saved player configs. */
    static int[] getCFG(Player p) {
        return playerHash.get(p.getUniqueId());
    }

}
