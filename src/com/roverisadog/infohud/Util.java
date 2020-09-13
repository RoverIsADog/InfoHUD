
package com.roverisadog.infohud;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/** Helper class. */
public class Util {
    //Minecraft release 1.XX
    static int apiVersion;

    //Plugin instance and currently running thread
    static BukkitTask task;
    private static InfoHUD plugin;

    //Player management
    private static HashMap<UUID, Map<String, Object>> playerHash;
    private static HashMap<Biome, Object> brightBiomes;

    //Default values
    static final String CMD_NAME = "infohud";
    static final String[] COORDS_OPTIONS = new String[]{"disabled", "enabled"};
    static final String[] TIME_OPTIONS = new String[]{"disabled", "ticks", "24 hours clock", "villager schedule"};
    static final String[] DARK_OPTIONS = new String[]{"disabled", "enabled", "auto"};

    static final String BRIGHT_BIOMES_PATH = "brightBiomes";
    static final String PLAYER_CFG_PATH = "playerConfig";
    static final String REFRESH_RATE_PATH = "refreshRate";

    //Shortcuts
    static final String PERM_USE = "infohud.use";
    static final String PERM_ADMIN = "infohud.admin";

    static final String HIGHLIGHT = ChatColor.YELLOW.toString();
    static final String SIGNATURE = ChatColor.DARK_AQUA.toString();
    static final String ERROR = ChatColor.RED.toString();

    static final String RES = ChatColor.RESET.toString();
    static final String WHI = ChatColor.WHITE.toString();
    static final String GLD = ChatColor.GOLD.toString();
    static final String DAQA = ChatColor.DARK_AQUA.toString();
    static final String DBLU = ChatColor.DARK_BLUE.toString();
    static final String GREN = ChatColor.GREEN.toString();

    //Performance
    private static long refreshRate;
    static long benchmark = 0;

    /** Loads disk contents of config.yml into memory. Returns false if an unhandled exception is found. */
    public static boolean loadConfig(Plugin plu) {
        try {
            plu.reloadConfig();
            Util.plugin = (InfoHUD) plu;
            Util.refreshRate = plu.getConfig().getLong(REFRESH_RATE_PATH);
            // Map< UUID , Map<String, Integer> > -- Loading players
            Util.playerHash = new HashMap<>();
            for (String playerStr : plu.getConfig().getConfigurationSection(PLAYER_CFG_PATH).getKeys(false)) {
                Util.playerHash.put(UUID.fromString(playerStr), plu.getConfig().getConfigurationSection(PLAYER_CFG_PATH + "." + playerStr).getValues(false) );
            }
            Util.brightBiomes = new HashMap<>(); //Loading biomes
            for (String cur : plu.getConfig().getStringList(BRIGHT_BIOMES_PATH)){
                try {
                    Biome bio = Biome.valueOf(cur);
                    Util.brightBiomes.put(bio, null);
                }
                catch (Exception ignored) {} //Biome misspelled, nonexistent or from future version
            }
            return true;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    /** Reloads content of config.yml */
    static boolean reload(){
        boolean b;
        try {
            task.cancel();
            b = loadConfig(plugin);
            task = plugin.start(Util.plugin);
            return b;
        }
        catch(Exception e){ return false; }
    }

    /* ---------------------------------------------------------------------- Player Management ---------------------------------------------------------------------- */

    /** Checks that the player is in the list.*/
    static boolean isEnabled(Player player) {
        return playerHash.containsKey(player.getUniqueId());
    }

    /** Saves player UUID into player list. */
    static String savePlayer(Player player) {
        //Putting default values
        HashMap<String, Object> defaultCfg = new HashMap<>();
        defaultCfg.put("coordinatesMode", 1); //1 : enabled
        defaultCfg.put("timeMode", 2); //2 : clock
        defaultCfg.put("darkMode", 2); //2 : auto
        playerHash.put(player.getUniqueId(), defaultCfg);
        //Saves changes
        plugin.getConfig().set(PLAYER_CFG_PATH + "." + player.getUniqueId().toString(), playerHash.get(player.getUniqueId()));
        plugin.saveConfig();
        return "InfoHUD is now " + (isEnabled(player) ? GREN + "enabled" : ERROR + "disabled") + RES + ".";
    }

    /** Removes player UUID from player list. */
    static String removePlayer(Player player) {
        playerHash.remove(player.getUniqueId());
        //Saves changes
        plugin.getConfig().set(PLAYER_CFG_PATH + "." + player.getUniqueId().toString(), null);
        plugin.saveConfig();
        return "InfoHUD is now " + (isEnabled(player) ? GREN + "enabled" : ERROR + "disabled") + RES + ".";
    }

    /* ---------------------------------------------------------------------- COORDINATES ---------------------------------------------------------------------- */

    /** Returns coordinates display settings for player. */
    static int getCoordinatesMode(Player p) {
        return (int) playerHash.get(p.getUniqueId()).get("coordinatesMode");
    }

    /** Changes coordinates mode and returns new mode. */
    static String setCoordinatesMode(Player p, int newMode){
        playerHash.get(p.getUniqueId()).put("coordinatesMode", newMode);
        //Saves changes
        plugin.getConfig().createSection(PLAYER_CFG_PATH + "." + p.getUniqueId().toString(), playerHash.get(p.getUniqueId()));
        plugin.saveConfig();
        return "Coordinates display set to: " + HIGHLIGHT + COORDS_OPTIONS[newMode] + RES + ".";
    }

    /** Returns string of player position. */
    static String getCoordinatesStr(Player p){
        return p.getLocation().getBlockX() + " " + p.getLocation().getBlockY() + " " + p.getLocation().getBlockZ();
    }

    /* ---------------------------------------------------------------------- COORDINATES ---------------------------------------------------------------------- */

    /** Returns time display settings for player. */
    static int getTimeMode(Player p) {
        return (int) playerHash.get(p.getUniqueId()).get("timeMode");
    }

    /** Changes time mode and returns new mode. */
    static String setTimeMode(Player p, int newMode){
        if (newMode == 3 &&  apiVersion < 14)
            return ERROR + "Villager schedule display is meaningless for versions before 1.14.";

        playerHash.get(p.getUniqueId()).put("timeMode", newMode);
        //Saves changes
        plugin.getConfig().createSection(PLAYER_CFG_PATH + "." + p.getUniqueId().toString(), playerHash.get(p.getUniqueId()));
        plugin.saveConfig();
        return "Time display set to: " + HIGHLIGHT + TIME_OPTIONS[newMode] + RES + ".";
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

    /* ---------------------------------------------------------------------- Dark Mode ---------------------------------------------------------------------- */

    /** Returns dark mode settings for player. */
    static int getDarkMode(Player p) {
        return (int) playerHash.get(p.getUniqueId()).get("darkMode");
    }

    /** Changes dark mode settings and returns new settings. */
    static String setDarkMode(Player p, int newMode){
        playerHash.get(p.getUniqueId()).put("darkMode", newMode);
        //Saves changes
        plugin.getConfig().createSection(PLAYER_CFG_PATH + "." + p.getUniqueId().toString(), playerHash.get(p.getUniqueId()));
        plugin.saveConfig();
        return "Dark mode set to: " + HIGHLIGHT + DARK_OPTIONS[newMode] + RES + ".";
    }

    /** Returns whether the player is in a bright biome for darkmode. */
    static boolean isInBrightBiome(Player p) {
        return brightBiomes.containsKey(p.getLocation().getBlock().getBiome());
    }

    /** Get a list of all bright biomes currently recognized. Not the same as all bright biomes
     * on file as some may not be recognized by older/newer minecraft versions. */
    static ArrayList<String> getBrightBiomesList(){
        ArrayList<String> biomeList = new ArrayList<>();
        for (Biome b : brightBiomes.keySet()) biomeList.add(b.toString());
        return biomeList;
    }

    static String addBrightBiome(Biome b) {
        try {
            //Already there
            if (plugin.getConfig().getStringList(BRIGHT_BIOMES_PATH).contains(b.toString())) {
                return HIGHLIGHT + b.toString() + RES + " is already in the bright biomes list.";
            }
            else {
                //Add to HashMap
                brightBiomes.put(b, null);
                //Update config.yml
                List<String> biomeList = new ArrayList<>(plugin.getConfig().getStringList(BRIGHT_BIOMES_PATH));
                biomeList.add(b.toString());
                plugin.getConfig().set(BRIGHT_BIOMES_PATH, biomeList);
                plugin.saveConfig();
                return GREN + "Added " + HIGHLIGHT + b.toString() + GREN + " to the bright biomes list.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ERROR + "Error while adding " + HIGHLIGHT + b.toString() + ERROR + " to bright biomes list.";
        }
    }

    static String removeBrightBiome(Biome b) {
        try {
            if (plugin.getConfig().getStringList(BRIGHT_BIOMES_PATH).contains(b.toString())) {
                //Update config.yml
                List<String> biomeList = new ArrayList<>(plugin.getConfig().getStringList(BRIGHT_BIOMES_PATH));
                biomeList.remove(b.toString());
                plugin.getConfig().set(BRIGHT_BIOMES_PATH, biomeList);
                plugin.saveConfig();
                //Remove from HashMap
                brightBiomes.remove(b);
                return GREN + "Removed " + HIGHLIGHT + b.toString() + GREN + " to the bright biomes list.";
            }
            else {
                return HIGHLIGHT + b.toString() + RES + " isn't in the bright biomes list.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ERROR + "Error while removing " + HIGHLIGHT + b.toString() + ERROR + " to the bright biomes list.";
        }
    }

    /* ---------------------------------------------------------------------- Direction  ---------------------------------------------------------------------- */

    /** Calculates direction the player is facing and returns corresponding string. */
    static String getPlayerDirection(Player player) {
        //-180: Leaning left | +180: Leaning right
        float yaw = player.getLocation().getYaw();
        //Bring to 360 degrees (Clockwise from -X axis)
        if (yaw < 0.0F)
            yaw += 360.0F;
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
            default:
                //Example: (359 + 22.5)/45
                return "S [+Z]";
        }
    }

    /* ---------------------------------------------------------------------- Admin ---------------------------------------------------------------------- */

    /** How many tick between each refresh. Values higher than 20 may lead to actionbar text fading. */
    static long getRefreshRate() {
        return refreshRate;
    }

    /** Change how many ticks between each refresh. */
    static String setRefreshRate(long newRate) {
        try {
            if (newRate == 0 || newRate > 40) return ERROR + "Number must be between 1 and 40 ticks.";
            refreshRate = newRate;
            //Saves value for next time
            plugin.getConfig().set("refreshRate", refreshRate);
            plugin.saveConfig();
            //Stop task and restart with new refresh rate
            task.cancel();
            task = plugin.start(Util.plugin);
            return "Refresh rate set to " + HIGHLIGHT + newRate + RES + ".";
        }
        catch (Exception e) {
            e.printStackTrace();
            return ERROR + "Error while changing refresh rate.";
        }
    }

    /** Shortcut to print to the console. */
    static void print(String msg) {
        Bukkit.getConsoleSender().sendMessage(Util.SIGNATURE + "[InfoHUD] " + Util.RES + msg);
    }

    static String getBenchmark(){
        return "InfoHUD took " + Util.HIGHLIGHT + Util.benchmark/(1000) + Util.RES +" ns (" + Util.HIGHLIGHT +
                String.format("%.3f", Util.benchmark/(50000000D) * (20D/Util.getRefreshRate())) + Util.RES + " ticks/second) during the last update.";
    }

    /*
    static void sendToActionBar(Player player, String msg){
        CraftPlayer p = (CraftPlayer) player;
        IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + msg + "\"}");
        PacketPlayOutChat ppoc = new PacketPlayOutChat(icbc, ChatMessageType.GAME_INFO, p.getUniqueId());
        p.getHandle().playerConnection.sendPacket(ppoc);
    }
     */

}
