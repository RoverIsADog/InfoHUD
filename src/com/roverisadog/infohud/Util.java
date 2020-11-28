
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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Helper class. */
public class Util {

    private Util() {
        throw new IllegalArgumentException();
    }

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
    public static String bright1 = Util.GLD;
    public static String bright2 = Util.WHI;
    public static String dark1 = Util.DBLU;
    public static String dark2 = Util.DAQA;

    /** Minecraft release 1.XX. */
    public static int apiVersion;
    /** CraftBukkit, Spigot, Paper, ... */
    public static String serverVendor;

    /** Currently loaded biomes considered bright. */
    private static EnumSet<Biome> brightBiomes;

    //Default values
    static final String CMD_NAME = "infohud";

    public static final String BRIGHT_BIOMES_PATH = "brightBiomes";
    public static final String PLAYER_CFG_PATH = "playerConfig";
    public static final String MESSAGE_UPDATE_DELAY_PATH = "messageUpdateDelay";
    public static final String BIOME_UPDATE_DELAY_PATH = "biomeUpdateDelay";
    public static final String VERSION_PATH = "infohudVersion";

    public static final String SIGNATURE = Util.SIGN + "[InfoHUD] " + Util.RES;

    public static final int DEFAULT_MESSAGE_UPDATE_DELAY = 5;
    public static final int DEFAULT_BIOME_UPDATE_DELAY = 40;


    //Performance
    /** Delay between each actionbar message update. */
    private static long messageUpdateDelay;
    /** Delay between each biome detection update. */
    private static long biomeUpdateDelay = 40L;

    static long benchmark = 0;

    /** Running plugin. */
    static InfoHUD plugin;

    static boolean isFromOlderVersion = false;

    /** Loads disk contents of config.yml into memory. Returns false if an unhandled exception is found. */
    static boolean loadConfig(InfoHUD instance) {
        try {

            instance.reloadConfig();

            //Get the message update delay.
            messageUpdateDelay = instance.getConfig().getLong(MESSAGE_UPDATE_DELAY_PATH);
            //Older versions
            if (messageUpdateDelay == 0L) {
                messageUpdateDelay = instance.getConfig().getLong("refreshRate");
            }

            //Get the biome update delay.
            biomeUpdateDelay = instance.getConfig().getLong(BIOME_UPDATE_DELAY_PATH);
            if (biomeUpdateDelay == 0L) {
                biomeUpdateDelay = 40L; //Default value.
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
                isFromOlderVersion = false;
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
    static void updateConfigFile(InfoHUD plugin) {
        String msg = GRN + "Old config file detected: updating...";

        //Saves old data into code.
        List<String> oldBiomeList = plugin.getConfig().getStringList(BRIGHT_BIOMES_PATH);
        messageUpdateDelay = plugin.getConfig().getLong("refreshRate"); //Old name

        //Set all config data to null, and save (wipe)
        for (String key : plugin.getConfig().getKeys(false)) {
            plugin.getConfig().set(key, null);
        }
        plugin.saveConfig();

        //Rewrite old data and save.
        plugin.getConfig().set(VERSION_PATH, plugin.getDescription().getVersion());
        plugin.getConfig().set(MESSAGE_UPDATE_DELAY_PATH, messageUpdateDelay);
        plugin.getConfig().set(BIOME_UPDATE_DELAY_PATH, DEFAULT_BIOME_UPDATE_DELAY); //Default value
        plugin.getConfig().set(BRIGHT_BIOMES_PATH, oldBiomeList);
        for (UUID id : PlayerCfg.playerHash.keySet()) {
            Util.plugin.getConfig().createSection(PLAYER_CFG_PATH + "." + id.toString(),
                    PlayerCfg.playerHash.get(id).toMap());
        }
        plugin.saveConfig();

        msg += " Done";
        printToTerminal(msg);
    }

    /* ---------------------------------------------------------------------- Dark Mode ---------------------------------------------------------------------- */

    /** Returns whether the player is in a bright biome for darkmode. */
    static void updateIsInBrightBiome(Player p) {
        PlayerCfg.playerHash.get(p.getUniqueId()).isInBrightBiome
                = brightBiomes.contains(p.getLocation().getBlock().getBiome());
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
                return GRN + "Removed " + HLT + b.toString() + GRN + " from the bright biomes list.";
            }
            //Wasn't included.
            else {
                return HLT + b.toString() + RES + " isn't in the bright biomes list.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ERR + "Error while removing " + HLT + b.toString() + ERR + " from the bright biomes list.";
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
        if (yaw < 0.0F) {
            yaw += 360.0F;
        }
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

    /** @return How many ticks between each message update. */
    static long getMessageUpdateDelay() {
        return messageUpdateDelay;
    }

    /** @return How many ticks between each biome change check. */
    static long getBiomeUpdateDelay() {
        return biomeUpdateDelay;
    }

    /**
     * Change how many ticks between each action bar message update.
     * Values higher than 20 may lead to actionbar text fading before it is updated.
     * Stops current updater task and schedules a new one with updated value.
     * @return Small message indicating updated state.
     */
    static boolean setMessageUpdateDelay(CommandSender sender, String[] args, int argStart) {
        //No number given
        if (args.length < argStart + 1) {
            sendMsg(sender, "Message update delay is currently: "
                    + Util.HLT + Util.getMessageUpdateDelay() + " ticks.");
        }
        //Number was given
        else {
            try {
                long newDelay = Long.parseLong(args[argStart]);

                if (newDelay <= 0 || newDelay > 40) {
                    sendMsg(sender, ERR + "Number must be between 1 and 40 ticks.");
                    return true;
                }
                messageUpdateDelay = newDelay;

                //Save the new value.
                plugin.getConfig().set(MESSAGE_UPDATE_DELAY_PATH, messageUpdateDelay);
                plugin.saveConfig();

                //Stop plugin and restart with new refresh period.
                plugin.msgSenderTask.cancel();
                plugin.msgSenderTask = plugin.startMessageUpdaterTask(plugin, getMessageUpdateDelay());

                sendMsg(sender, "Message update delay set to " + HLT + newDelay + RES + ".");

                return true;
            } catch (NumberFormatException e) {
                sendMsg(sender, Util.ERR + "Must be a positive integer between 1 and 40.");
            }
        }
        return true;
    }

    /** Reloads content of config.yml. */
    static boolean reload(CommandSender sender) {
        boolean success;
        try {
            //Cancel task, reload config, and restart task.
            plugin.msgSenderTask.cancel();
            success = loadConfig(plugin);
            plugin.msgSenderTask = plugin.startMessageUpdaterTask(plugin, getMessageUpdateDelay());

            if (success) {
                sendMsg(sender, Util.GRN + "Reloaded successfully.");
            }
            else {
                sendMsg(sender, Util.ERR + "Reload failed.");
            }

            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    /** Shortcut to print to the console. */
    static void printToTerminal(String msg) {
        Bukkit.getConsoleSender().sendMessage(SIGNATURE + msg);
    }

    /**
     * Gets how much time the last update took.
     * @return Small message indicating status.
     */
    static boolean getBenchmark(CommandSender sender) {
        sendMsg(sender, "InfoHUD took " + Util.HLT + String.format("%.3f", Util.benchmark / (1000000D)) + Util.RES
                + " ms (" + Util.HLT + String.format("%.2f", (Util.benchmark / (10000D)) / 50D)
                + Util.RES + " % tick) during the last update.");
        return true;
    }

    /** Send chat message to command sender. */
    static void sendMsg(CommandSender sender, String msg) {
        sender.sendMessage(SIGN + "[InfoHUD] " + RES + msg);
    }
}
