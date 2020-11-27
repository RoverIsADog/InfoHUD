
package com.roverisadog.infohud;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.roverisadog.infohud.command.CoordMode;
import com.roverisadog.infohud.command.DarkMode;
import com.roverisadog.infohud.command.TimeMode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

/** Helper class. */
public class Util {

    //Shortcuts
    public static final String PERM_USE = "infohud.use";
    public static final String PERM_ADMIN = "infohud.admin";

    public static final String HLT = ChatColor.YELLOW.toString();
    public static final String SIGN = ChatColor.DARK_AQUA.toString();
    public static final String ERR = ChatColor.RED.toString();

    public static final String RES = ChatColor.RESET.toString();
    public static final String WHI = ChatColor.WHITE.toString();
    public static final String GLD = ChatColor.GOLD.toString();
    public static final String DAQA = ChatColor.DARK_AQUA.toString();
    public static final String DBLU = ChatColor.DARK_BLUE.toString();
    public static final String GRN = ChatColor.GREEN.toString();

    //Colors
    public static String Bright1 = Util.GLD;
    public static String Bright2 = Util.WHI;
    public static String Dark1 = Util.DBLU;
    public static String Dark2 = Util.DAQA;


    //Minecraft release 1.XX
    public static int apiVersion;
    public static String serverVendor;

    private static EnumSet<Biome> brightBiomes;

    //Default values
    static final String CMD_NAME = "infohud";

    static final String BRIGHT_BIOMES_PATH = "brightBiomes";
    static final String PLAYER_CFG_PATH = "playerConfig";
    static final String REFRESH_PERIOD_PATH = "refreshPeriod";
    static final String VERSION_PATH = "infohudVersion";

    static final String SIGNATURE = Util.SIGN + "[InfoHUD] " + Util.RES;

    //Performance
    private static long refreshPeriod;
    static long benchmark = 0;

    /** Running plugin. */
    static InfoHUD plugin;

    static boolean isFromOlderVersion = false;

    /** Loads disk contents of config.yml into memory. Returns false if an unhandled exception is found. */
    static boolean loadConfig(InfoHUD instance) {
        try {

            instance.reloadConfig();

            //Get the refresh period
            refreshPeriod = instance.getConfig().getLong(REFRESH_PERIOD_PATH);
            //Older versions
            if (refreshPeriod == 0L) {
                refreshPeriod = instance.getConfig().getLong("refreshRate");
            }

            //Building player settings hash
            PlayerCfg.playerHash = new HashMap<>(); //Map<UUID , PlayerConfig>

            //For every section of "playerCfg" : UUID in string form
            for (String playerStr : instance.getConfig().getConfigurationSection(PLAYER_CFG_PATH).getKeys(false)) {

                //Get UUID from String
                UUID playerID = UUID.fromString(playerStr);

                //Get raw mapping of the player's settings
                Map<String, Object> playerSettings = instance.getConfig()
                        .getConfigurationSection(PLAYER_CFG_PATH + "." + playerStr).getValues(false);

                //Decode into enumerated types.
                PlayerCfg playerCfg = loadPlayerSettings(playerID, playerSettings);

                //Translate into faster mappings
                PlayerCfg.playerHash.put(playerID, playerCfg);
            }

            //Loading biomes
            brightBiomes = EnumSet.noneOf(Biome.class);
            for (String currentBiome : instance.getConfig().getStringList(BRIGHT_BIOMES_PATH)) {
                try {
                    Biome bio = Biome.valueOf(currentBiome);
                    brightBiomes.add(bio);
                }
                catch (Exception ignored) {
                    //Biome misspelled, nonexistent, from future or past version.
                }
            }

            if (isFromOlderVersion) {
                updateConfigFile(instance);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Loads a specific player's configuration from a Map.
     * @param map Map to read from.
     * @return PlayerCfg containing the desired values.
     */
    static PlayerCfg loadPlayerSettings(UUID id, Map<String, Object> map) {

        CoordMode coordMode;
        TimeMode timeMode;
        DarkMode darkMode;

        //Is from a version before InfoHUD 1.2 (Stored as int)
        if (map.get(CoordMode.cfgKey) instanceof Integer) {
            isFromOlderVersion = true;
            coordMode = CoordMode.get((int) map.get(CoordMode.cfgKey));
            timeMode = TimeMode.get((int) map.get(TimeMode.cfgKey));
            darkMode = DarkMode.get((int) map.get(DarkMode.cfgKey));
        }
        //Is from newer versions (stored as String)
        else {
            coordMode = CoordMode.get((String) map.get(CoordMode.cfgKey));
            timeMode = TimeMode.get((String) map.get(TimeMode.cfgKey));
            darkMode = DarkMode.get((String) map.get(DarkMode.cfgKey));
        }

        return new PlayerCfg(id, coordMode, timeMode, darkMode);
    }

    /** Updates old config file to new format. */
    static void updateConfigFile(InfoHUD instance) {
        String msg = GRN + "Old config file detected: updating...";
        //Saves old data into code.
        List<String> oldBiomeList = instance.getConfig().getStringList(BRIGHT_BIOMES_PATH);
        Long oldRefreshPeriod = instance.getConfig().getLong("refreshRate"); //Old name

        //Set all config data to null, and save (wipe)
        for (String key : instance.getConfig().getKeys(false)) {
            instance.getConfig().set(key, null);
        }
        instance.saveConfig();

        //Rewrite old data and save.
        instance.getConfig().set(VERSION_PATH, instance.getDescription().getVersion());
        instance.getConfig().set(REFRESH_PERIOD_PATH, oldRefreshPeriod);
        instance.getConfig().set(BRIGHT_BIOMES_PATH, oldBiomeList);
        for (UUID id : PlayerCfg.playerHash.keySet()) {
            plugin.getConfig().createSection(PLAYER_CFG_PATH + "." + id.toString(), PlayerCfg.playerHash.get(id).toMap());
        }
        instance.saveConfig();
        isFromOlderVersion = false;
        msg += " Done";
        print(msg);
    }

    /** Reloads content of config.yml. */
    static boolean reload() {
        boolean b;
        try {
            plugin.task.cancel();
            b = loadConfig(plugin);
            plugin.task = plugin.start(plugin);
            return b;
        }
        catch (Exception e) {
            return false;
        }
    }

    /* ---------------------------------------------------------------------- TIME ---------------------------------------------------------------------- */

    /** Converts minecraft internal clock to HH:mm string. */
    public static String getTime24(long time) {
        //MC day starts at 6:00: https://minecraft.gamepedia.com/Daylight_cycle
        String timeH = Long.toString((time / 1000L + 6L) % 24L);
        String timeM = String.format("%02d", time % 1000L * 60L / 1000L);
        return timeH + ":" + timeM;
    }

    public static String getTime12(long time) {
        //MC day starts at 6:00: https://minecraft.gamepedia.com/Daylight_cycle
        boolean isPM = false;
        long currentHour = (time / 1000L + 6L) % 24L;
        if (currentHour > 12) {
            currentHour -= 12L;
            isPM = true;
        }
        String timeH = Long.toString(currentHour);
        String timeM = String.format("%02d", time % 1000L * 60L / 1000L);
        return timeH + ":" + timeM + (isPM ? " PM" : " AM");
    }

    /**
     * Returns current villager behavior and the time remaining until the next
     * scheduled behavior.
     * @param col1 Main color.
     * @param col2 Secondary color.
     */
    public static String getVillagerTimeLeft(long time, String col1, String col2) {
        //Sleeping 12000 - 0
        if (time > 12000L) {
            long remaining = 12000L - time + 12000L;
            return col1 + "Sleep: " + col2 + remaining / 1200L + ":" + String.format("%02d", remaining % 1200L / 20L);
        }
        //Wandering 11000 - 12000
        else if (time > 11000L) {
            long remaining = 1000L - time + 11000L;
            return col1 + "Wander: " + col2 + 0 + ":" + String.format("%02d", remaining % 1200L / 20L);
        }
        //Gathering 9000 - 11000
        else if (time > 9000L) {
            long remaining = 2000L - time + 9000L;
            return col1 + "Gather: " + col2 + remaining / 1200L + ":" + String.format("%02d", remaining % 1200L / 20L);
        }
        //Working 2000 - 9000
        else if (time > 2000L) {
            long remaining = 7000L - time + 2000L;
            return col1 + "Work: " + col2 + remaining / 1200L + ":" + String.format("%02d", remaining % 1200L / 20L);
        }
        //Wandering 0 - 2000
        else {
            long remaining = 2000L - time;
            return col1 + "Wander: " + col2 + remaining / 1200L + ":" + String.format("%02d", remaining % 1200L / 20L);
        }
    }

    /* ---------------------------------------------------------------------- Dark Mode ---------------------------------------------------------------------- */

    /** Returns whether the player is in a bright biome for darkmode. */
    static void updateIsInBrightBiome(Player p) {
        PlayerCfg.playerHash.get(p.getUniqueId()).isInBrightBiome = brightBiomes.contains(p.getLocation().getBlock().getBiome());
    }

    /**
     * Get a list of all bright biomes currently recognized. Not the same as
     * all bright biomes on the config file as some may not be recognized by
     * older/newer minecraft versions.
     */
    static List<String> getBrightBiomesList() {
        return brightBiomes.stream()
                .map(Biome::toString)
                .collect(Collectors.toList());
    }

    /**
     * Adds a biome from the bright biomes list.
     * @return Small message confirming status.
     */
    static String addBrightBiome(Biome b) {
        try {
            //Already there
            if (brightBiomes.contains(b)) {
                return HLT + b.toString() + RES + " is already included in the bright biomes list.";
            }
            else {
                //Add to set
                brightBiomes.add(b);
                //Update config.yml. APPEND MODE to not lose biomes from other versions.
                List<String> biomeList = new LinkedList<>(plugin.getConfig().getStringList(BRIGHT_BIOMES_PATH));
                biomeList.add(b.toString());
                plugin.getConfig().set(BRIGHT_BIOMES_PATH, biomeList);
                plugin.saveConfig();
                return GRN + "Added " + HLT + b.toString() + GRN + " to the bright biomes list.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ERR + "Error while adding " + HLT + b.toString() + ERR + " to bright biomes list.";
        }
    }

    /**
     * Removes a biome from the bright biomes list.
     * @return Small message confirming status.
     */
    static String removeBrightBiome(Biome b) {
        try {
            //Contained
            if (brightBiomes.contains(b)) {
                //Update config.yml. APPEND MODE to not lose biomes from other versions.
                List<String> biomeList = new LinkedList<>(plugin.getConfig().getStringList(BRIGHT_BIOMES_PATH));
                biomeList.remove(b.toString());
                plugin.getConfig().set(BRIGHT_BIOMES_PATH, biomeList);
                plugin.saveConfig();

                //Remove from set
                brightBiomes.remove(b);
                return GRN + "Removed " + HLT + b.toString() + GRN + " to the bright biomes list.";
            }
            //Wasn't included.
            else {
                return HLT + b.toString() + RES + " isn't in the bright biomes list.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ERR + "Error while removing " + HLT + b.toString() + ERR + " to the bright biomes list.";
        }
    }

    /* ---------------------------------------------------------------------- Direction  ---------------------------------------------------------------------- */

    /**
     * Calculates cardinal direction the player is facing.
     * @return Small message indicating cardinal direction and coordinate
     *         towards which the player is facing.
     */
    static String getPlayerDirection(Player player) {
        //-180: Leaning left | +180: Leaning right
        float yaw = player.getLocation().getYaw();
        //Bring to 360 degrees (Clockwise from -X axis)
        if (yaw < 0.0F) yaw += 360.0F;
        //Separate into 8 sectors (Arc: 45deg), offset by 1/2 sector (Arc: 22.5deg)
        int sector = (int) ((yaw + 22.5F) / 45F);
        switch (sector) {
            case 1: return "SW";
            case 2: return "W [-X]";
            case 3: return "NW";
            case 4: return "N [-Z]";
            case 5: return "NE";
            case 6: return "E [+X]";
            case 7: return "SE";
            case 0:
            default: //Example: (359 + 22.5)/45
                return "S [+Z]";
        }
    }

    /* ---------------------------------------------------------------------- Admin ---------------------------------------------------------------------- */

    /** @return How many ticks between each refresh. */
    static long getRefreshPeriod() {
        return refreshPeriod;
    }

    /**
     * Change how many ticks between each refresh. Values higher than 20 may lead to actionbar text fading.
     * Stops current task and starts a new one with updated value.
     * @return Small message indicating updated state.
     */
    static String setRefreshPeriod(long newPeriod) {
        try {
            if (newPeriod <= 0 || newPeriod > 40) {
                return ERR + "Number must be between 1 and 40 ticks.";
            }
            refreshPeriod = newPeriod;

            //Save the new value.
            plugin.getConfig().set(REFRESH_PERIOD_PATH, refreshPeriod);
            plugin.saveConfig();

            //Stop plugin and restart with new refresh period.
            plugin.task.cancel();
            plugin.task = plugin.start(plugin);
            return "Refresh rate set to " + HLT + newPeriod + RES + ".";
        }
        catch (Exception e) {
            e.printStackTrace();
            return ERR + "Error while changing refresh rate.";
        }
    }

    /** Shortcut to print to the console. */
    static void print(String msg) {
        Bukkit.getConsoleSender().sendMessage(SIGNATURE + msg);
    }

    /**
     * Gets how much time the last update took.
     * @return Small message indicating status.
     */
    static String getBenchmark() {
        return "InfoHUD took " + Util.HLT + String.format("%.3f", Util.benchmark / (1000000D)) + Util.RES
                + " ms (" + Util.HLT + String.format("%.2f", (Util.benchmark / (10000D)) / 50D)
                + Util.RES + " % tick) during the last update.";
    }

    static int getNumber() {
        return PlayerCfg.playerHash.size();
    }

}
