
package com.roverisadog.infohud;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class Util {
    //Configuration files
    private static Plugin plugin;

    //Player management
    private static HashMap<UUID, int[]> playerHash;
    private static List<String> cfgStringList;
    static HashMap<String, Object> brightBiomes;

    //Default values
    static final String CMD_NAME = "infohud";
    static final int[] DEFAULT_CFG = new int[]{1, 2, 2}; //[coordsMode, timeMode, darkModeCFG]
    static final String[] COORDS_OPTIONS = new String[]{"disabled", "enabled"};
    static final String[] TIME_OPTIONS = new String[]{"disabled", "ticks", "24 hours clock", "villager schedule"};
    static final String[] DARK_OPTIONS = new String[]{"disabled", "enabled", "auto"};

    //Shortcuts
    static final String PERM_USE = "infohud.use";
    static final String PERM_ADMIN = "infohud.admin";
    static String RES = ChatColor.RESET.toString();
    static String YEL = ChatColor.YELLOW.toString();
    static String WHI = ChatColor.WHITE.toString();
    static String GLD = ChatColor.GOLD.toString();
    static String BLU = ChatColor.BLUE.toString();
    static String DAQA = ChatColor.DARK_AQUA.toString();
    static String AQA = ChatColor.AQUA.toString();
    static String RED = ChatColor.RED.toString();
    static String GRA = ChatColor.GRAY.toString();
    static String DBLU = ChatColor.DARK_BLUE.toString();

    private static long refreshRate;

    /** Loads contents of config.yml into memory. */
    public static void readConfig(Plugin plu) {
        Util.plugin = plu;

        Util.refreshRate = plu.getConfig().getLong("refreshRate");
        List<String> tmp = plu.getConfig().getStringList("brightBiomes");
        brightBiomes = new HashMap<>();
        for (String cur : tmp){
            brightBiomes.put(cur, null);
        }

        //Initialize string list
        Util.cfgStringList = plu.getConfig().getStringList("playerConfig");

        //UUID, coordEnabled, timeEnabled, tickEnabled, villagerMode
        Util.playerHash = new HashMap<>();

        //Loading player config into hashmap
        for (String currentPlayer : cfgStringList) {
            String[] s = currentPlayer.split(", ");
            playerHash.put(UUID.fromString(s[0]), new int[]{Integer.parseInt(s[1]),Integer.parseInt(s[2]), Integer.parseInt(s[3])});
        }
    }

    /** Checks that the player is in the list.*/
    static boolean isOnList(Player player) {
        return playerHash.containsKey(player.getUniqueId());
    }

    /** Saves player UUID into player list. */
    static String savePlayer(Player player) {
        //Save into hashmap and string list
        playerHash.put(player.getUniqueId(), DEFAULT_CFG);
        cfgStringList.add(player.getUniqueId().toString() + ", " + DEFAULT_CFG[0] + ", " + DEFAULT_CFG[1] + ", " + DEFAULT_CFG[2]);

        //Saves changes
        plugin.getConfig().set("playerConfig", cfgStringList);
        plugin.saveConfig();

        return "InfoHUD is now " + (Util.isOnList(player) ? "enabled" : "disabled") + ".";
    }

    /** Removes player UUID from player list. */
    static String removePlayer(Player player) {
        //Remove from hashmap and string list
        int[] removedCFG = playerHash.remove(player.getUniqueId());
        cfgStringList.remove(player.getUniqueId().toString() + ", " + removedCFG[0] + ", " + removedCFG[1] + ", " + removedCFG[2]);

        //Saves changes
        plugin.getConfig().set("playerConfig", cfgStringList);
        plugin.saveConfig();

        return "InfoHUD is now " + (Util.isOnList(player) ? "enabled" : "disabled") + ".";
    }

    static String setCoordMode(Player p, int newMode){
        int[] cfg = playerHash.get(p.getUniqueId());
        cfg[0] = newMode;
        //Saves changes
        plugin.getConfig().set("playerConfig", cfgStringList);
        plugin.saveConfig();
        return "Coordinates display set to: " + COORDS_OPTIONS[newMode];
    }

    static String setTimeMode(Player p, int newMode){
        int[] cfg = playerHash.get(p.getUniqueId());
        cfg[1] = newMode;
        //Saves changes
        plugin.getConfig().set("playerConfig", cfgStringList);
        plugin.saveConfig();
        return "Time display set to: " + TIME_OPTIONS[newMode];
    }

    static String setDarkMode(Player p, int newMode){
        int[] cfg = playerHash.get(p.getUniqueId());
        cfg[2] = newMode;
        //Saves changes
        plugin.getConfig().set("playerConfig", cfgStringList);
        plugin.saveConfig();
        return "Time display set to: " + DARK_OPTIONS[newMode];
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
    static String getVillagerTimeLeft(long time, String col1, String col2){
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
        //-180: Leaning left
        //+180: Leaning right
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

    /** Change how many ticks between each refresh. */
    static String changeUpdateRate(long newRate) {
        if (newRate == 0 || newRate > 40) return "Number must be between 1 and 40 ticks";
        refreshRate = newRate;
        plugin.getConfig().set("refreshRate", refreshRate);
        plugin.saveConfig();
        return "Refresh rate set to " + newRate;
    }

    /** Returns int array of saved player configs. */
    static int[] getCFG(Player p) {
        return playerHash.get(p.getUniqueId());
    }

}
