
package com.roverisadog.infohud;

import com.roverisadog.infohud.command.CoordMode;
import com.roverisadog.infohud.command.DarkMode;
import com.roverisadog.infohud.command.TimeMode;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public class InfoHUD extends JavaPlugin {

    protected static InfoHUD instance;

    public InfoHUD() {
        instance = this;
    }

    private static boolean isSpigot;

    //import net.minecraft.server.v1_16_R2.ChatMessageType;
    //import net.minecraft.server.v1_16_R2.IChatBaseComponent;
    //import net.minecraft.server.v1_16_R2.PacketPlayOutChat;
    //import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;

    //Version for reflection
    private static String versionStr;
    //Reflected objects for NMS packets.
    private static Class<?> craftPlayerClass;
    private static Field playerConnectionField;
    private static Method getHandleMethod, sendPacketMethod;
    private static Constructor<?> packetPlayOutChatConstructor, chatMessageConstructor;
    private static Object charMessageTypeEnum;

    /** Time elapsed for the last update. */
    private long benchmarkStart;

    protected BukkitTask msgSenderTask;
    protected BukkitTask biomeUpdateTask;

    /**
     * Initial setup:
     * Load config, get version, get NMS packet classes.
     * Configure CommandHandler
     */
    @Override
    public void onEnable() {
        try {
            Util.printToTerminal(Util.GRN + "InfoHUD Enabling...");

            //Save initial cfg or load.
            this.saveDefaultConfig(); //Silent fails if config.yml already exists
            if (!Util.loadConfig()) {
                throw new Exception(Util.ERR + "Error while reading config.yml.");
            }

            //Version check
            //Eg: org.bukkit.craftbukkit.v1_16_R2.blabla
            String ver = Bukkit.getServer().getClass().getPackage().getName();
            versionStr = ver.split("\\.")[3];
            Util.apiVersion = Integer.parseInt(versionStr.split("_")[1]);
            Util.serverVendor = ver.split("\\.")[2];

            //Attempt to get version-specific NMS packets class.
            if (!setupPackets()) {
                throw new Exception(Util.ERR + "Version error.");
            }

            //Setup command executor
            this.getCommand(Util.CMD_NAME).setExecutor(new CommandExecutor(this));

            //Start sender and biome updater tasks
            msgSenderTask = startMessageUpdaterTask(Util.getMessageUpdateDelay());
            biomeUpdateTask = startBiomeUpdaterTask(Util.getBiomeUpdateDelay());


            Util.printToTerminal(Util.GRN + "InfoHUD Successfully Enabled on " + Util.WHI + (isSpigot ? "Spigot API" : "NMS") + " Version 1." + Util.apiVersion);
        }
        catch (Exception e) {
            Util.printToTerminal(e.getMessage());
            Util.printToTerminal("Shutting down...");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    /** Clean up while shutting down (Currently nothing). */
    @Override
    public void onDisable() {
        Util.printToTerminal(Util.GRN + "InfoHUD Disabled");
    }

    /**
     * Starts task whose job is to get each player's config and send the right
     * message accordingly. Does NOT change any value from {@link PlayerCfg}.
     * @return BukkitTask created.
     */
    public BukkitTask startMessageUpdaterTask(long refreshPeriod) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(instance, () -> {
            benchmarkStart = System.nanoTime();
            for (Player p : instance.getServer().getOnlinePlayers()) {

                //Skip players that are not on the list
                if (!PlayerCfg.isEnabled(p)) {
                    continue;
                }

                //Assumes that online players << saved players

                PlayerCfg cfg = PlayerCfg.getConfig(p);

                if (cfg.coordMode == CoordMode.DISABLED && cfg.timeMode == TimeMode.DISABLED) {
                    PlayerCfg.removePlayer(p);
                    continue;
                }

                //Setting dark mode colors -> Assume disabled : 0
                String color1; //Text
                String color2; //Values

                if (cfg.darkMode == DarkMode.AUTO) {
                    if (cfg.isInBrightBiome) {
                        color1 = Util.dark1;
                        color2 = Util.dark2;
                    }
                    else {
                        color1 = Util.bright1;
                        color2 = Util.bright2;
                    }
                }
                else if (cfg.darkMode == DarkMode.DISABLED) {
                    color1 = Util.bright1;
                    color2 = Util.bright2;
                }
                else { //DarkMode.ENABLED
                    color1 = Util.dark1;
                    color2 = Util.dark2;
                }

                //Coordinates enabled
                if (cfg.coordMode == CoordMode.ENABLED) {
                    switch (cfg.timeMode) {
                        case DISABLED:
                            sendToActionBar(p, color1 + "XYZ: "
                                    + color2 + CoordMode.getCoordinates(p) + " "
                                    + color1 + Util.getPlayerDirection(p));
                            break;
                        case CURRENT_TICK:
                            sendToActionBar(p, color1 + "XYZ: "
                                    + color2 + CoordMode.getCoordinates(p) + " "
                                    + color1 + String.format("%-10s", Util.getPlayerDirection(p))
                                    + color2 + TimeMode.getTimeTicks(p));
                            break;
                        case CLOCK24:
                            sendToActionBar(p, color1 + "XYZ: "
                                    + color2 + CoordMode.getCoordinates(p) + " "
                                    + color1 + String.format("%-10s", Util.getPlayerDirection(p))
                                    + color2 + TimeMode.getTime24(p));
                            break;
                        case CLOCK12:
                            sendToActionBar(p, color1 + "XYZ: "
                                    + color2 + CoordMode.getCoordinates(p) + " "
                                    + color1 + String.format("%-10s", Util.getPlayerDirection(p))
                                    + color2 + TimeMode.getTime12(p, color1, color2));
                            break;
                        case VILLAGER_SCHEDULE:
                            sendToActionBar(p, color1 + "XYZ: "
                                    + color2 + CoordMode.getCoordinates(p) + " "
                                    + color1 + String.format("%-10s", Util.getPlayerDirection(p))
                                    + color2 + TimeMode.getVillagerTime(p, color1, color2));
                            break;
                        default: //Ignored
                    }
                }

                //Coordinates disabled
                else if (cfg.coordMode == CoordMode.DISABLED) {
                    switch (cfg.timeMode) {
                        case CURRENT_TICK:
                            sendToActionBar(p, color2 + TimeMode.getTimeTicks(p));
                            break;
                        case CLOCK12:
                            sendToActionBar(p, color2 + TimeMode.getTime12(p, color1, color2));
                            break;
                        case CLOCK24:
                            sendToActionBar(p, color2 + TimeMode.getTime24(p));
                            break;
                        case VILLAGER_SCHEDULE:
                            sendToActionBar(p, color2 + TimeMode.getVillagerTime(p, color1, color2));
                            break;
                        default: //Ignored
                    }
                }
            }
            Util.benchmark = System.nanoTime() - benchmarkStart;
        }, 0L, refreshPeriod);
    }

    /**
     * Runs expensive tasks in a new thread (for now, biome fetching).
     */
    public BukkitTask startBiomeUpdaterTask(long refreshPeriod) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(instance, () -> {

            for (Player p : instance.getServer().getOnlinePlayers()) {
                if (PlayerCfg.isEnabled(p)) {
                    if (PlayerCfg.getConfig(p).darkMode == DarkMode.AUTO) {
                        Util.updateIsInBrightBiome(p);
                    }
                }
            }

        }, 0L, refreshPeriod);
    }

    /**
     *  Uses reflection to get version specific NMS packet-related classes.
     *  Preferred to Spigot built in method for wider compatibility.
     */
    private static boolean setupPackets() {

        // Use spigot API when possible
        try {
            Player.Spigot.class.getDeclaredMethod("sendMessage", ChatMessageType.class, BaseComponent.class);
            // Is using spigot / paper: No need to use reflection.
            isSpigot = true;
            Util.printToTerminal(Util.GRN + "Using Spigot API");
            return true;
        } catch (NoClassDefFoundError | Exception e) {
            // Is using bukkit: Use NMS
            isSpigot = false;
            Util.printToTerminal(Util.GRN + "Spigot API unavailable or incompatible: falling back to NMS");
        }

        // Use NMS Otherwise

        /* VERSION SPECIFIC PSEUDOCODE
        static void sendToActionBar(Player player, String msg) {
            CraftPlayer p = (CraftPlayer) player;
            IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + msg + "\"}");
            PacketPlayOutChat ppoc = new PacketPlayOutChat(icbc, ChatMessageType.GAME_INFO, UUID);
            p.getHandle().playerConnection.sendPacket(ppoc);
        }
         */
        try {
            // 1.8 - 1.16: net.minecraft.server.v1_16_R2.X
            // 1.17 - ?.??: net.minecraft.X TODO Check if changed again in future
            String nmsPath = Util.apiVersion < 17
                    ? "net.minecraft.server." + versionStr + "." // 1.8 - 1.16
                    : "net.minecraft."; // 1.17

            //org.bukkit.craftbukkit.VERSION.entity.CraftPlayer; | CraftPlayer p = (CraftPlayer) player;
            craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + versionStr + ".entity.CraftPlayer");

            /* import net.minecraft.server.v1_16_R2.IChatBaseComponent;
            1.8 - 1.16: net.minecraft.v1_16_R2.IChatBaseComponent
            1.17 - ?.??: net.minecraft.network.chat.IChatBaseComponent; //TODO CHECK IF CHANGED
             */
            Class<?> iChatBaseComponentClass;
            if (Util.apiVersion < 17)
                iChatBaseComponentClass = Class.forName(nmsPath + "IChatBaseComponent");
            else
                iChatBaseComponentClass = Class.forName(nmsPath + "network.chat.IChatBaseComponent");

            Util.printToTerminal("Checkpoint 1");

            /* Used to get Player.sendPackets -> p.getHandle().playerConnection.sendPacket(ppoc);
               PPOC is instance of packet.
               1.8 - 1.16: net.minecraft.v1_16_R2.Packet
               1.17 - ?.??: net.minecraft.network.protocol.Packet //TODO CHECK IF CHANGED
             */
            Class<?> packetClass;
            if (Util.apiVersion < 17)
                packetClass = Class.forName(nmsPath + "Packet");
            else
                packetClass = Class.forName(nmsPath + "network.protocol.Packet");

            Util.printToTerminal("Checkpoint 2");

            /* Get methods and fields from sending packet line:
                1.8 - 1.16: p.getHandle().playerConnection.sendPacket(ppoc);
                1.17 - ?.??: p.getHandle().b.sendPacket(ppoc); //TODO CHECK IF CHANGED
             */
            if (Util.apiVersion < 17) {
                getHandleMethod = craftPlayerClass.getMethod("getHandle");
                Util.printToTerminal("Checkpoint 2.11");
                playerConnectionField = getHandleMethod.getReturnType().getField("playerConnection");
                Util.printToTerminal("Checkpoint 2.12");
                sendPacketMethod = playerConnectionField.getType().getMethod("sendPacket", packetClass);
                Util.printToTerminal("Checkpoint 2.13");
            }
            else {
                getHandleMethod = craftPlayerClass.getMethod("getHandle");
                Util.printToTerminal("Checkpoint 2.21");
                playerConnectionField = getHandleMethod.getReturnType().getField("b");
                Util.printToTerminal("Checkpoint 2.22");
                sendPacketMethod = playerConnectionField.getType().getMethod("sendPacket", packetClass);
                Util.printToTerminal("Checkpoint 2.23");
            }

            Util.printToTerminal("Checkpoint 3");

            /* IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + msg + "\"}");
               1.8 - 1.16: net.minecraft.v1_16_R2.ChatMessage
               1.17 - ?.??: net.minecraft.network.chat.ChatMessage //TODO CHECK IF CHANGED
             */
            Class<?> chatMessageClass;
            if (Util.apiVersion < 17)
                chatMessageClass = Class.forName(nmsPath + "ChatMessage");
            else
                chatMessageClass = Class.forName(nmsPath + "network.chat.ChatMessage");
            chatMessageConstructor = chatMessageClass.getConstructor(String.class, Object[].class);

            Util.printToTerminal("Checkpoint 4");

            /*import net.minecraft.server.VERSION.IChatBaseComponent;
              PacketPlayOutChat ppoc = new PacketPlayOutChat(icbc, ChatMessageType.GAME_INFO, p.getUniqueId());
               1.8 - 1.16: net.minecraft.v1_16_R2.PacketPlayOutChat
               1.17 - ?.??: net.minecraft.network.protocol.game.PacketPlayOutChat //TODO CHECK IF CHANGED
            */
            Class<?> packetPlayOutChatClass;
            if (Util.apiVersion < 17)
                packetPlayOutChatClass = Class.forName(nmsPath+ "PacketPlayOutChat");
            else
                packetPlayOutChatClass = Class.forName(nmsPath+ "network.protocol.game.PacketPlayOutChat");

            Util.printToTerminal("Checkpoint 5");

            Class<?> chatMessageTypeClass;

            //1.8 - 1.11
            if (Util.apiVersion < 12) {
                // 1.8 - 1.11 : PacketPlayOutChat(IChatBaseComponent, byte)
                packetPlayOutChatConstructor = packetPlayOutChatClass
                        .getConstructor(iChatBaseComponentClass, byte.class);
            }
            //1.12 - 1.15
            else if (Util.apiVersion < 16) {
                // import net.minecraft.server.v1_16_R2.ChatMessageType;
                chatMessageTypeClass = Class
                        .forName(nmsPath + "ChatMessageType"); //Nonexistent on 1.8
                // ChatMessageType.GAME_INFO -> 2
                // PacketPlayOutChat ppoc = new PacketPlayOutChat(icbc, ChatMessageType.GAME_INFO, p.getUniqueId());
                charMessageTypeEnum = chatMessageTypeClass.getEnumConstants()[2];
                //1.12 - 1.15 : PacketPlayOutChat(IChatBaseComponent, ChatMessageType)
                packetPlayOutChatConstructor = packetPlayOutChatClass
                        .getConstructor(iChatBaseComponentClass, chatMessageTypeClass);
            }
            //1.16
            else if (Util.apiVersion < 17) {
                //import net.minecraft.server.v1_16_R2.ChatMessageType;
                chatMessageTypeClass = Class
                        .forName(nmsPath + "ChatMessageType"); //Nonexistent on 1.8
                //ChatMessageType.GAME_INFO -> 2nd index
                //PacketPlayOutChat ppoc = new PacketPlayOutChat(icbc, ChatMessageType.GAME_INFO, p.getUniqueId());
                charMessageTypeEnum = chatMessageTypeClass.getEnumConstants()[2];
                //1.16 - ?.?? : PacketPlayOutChat(IChatBaseComponent, ChatMessageType, UUID)
                packetPlayOutChatConstructor = packetPlayOutChatClass
                        .getConstructor(iChatBaseComponentClass, chatMessageTypeClass, UUID.class);
            }
            //1.17
            else {
                //import net.minecraft.network.chat.ChatMessageType;
                chatMessageTypeClass = Class
                        .forName(nmsPath + "network.chat.ChatMessageType"); //Nonexistent on 1.8
                //ChatMessageType.GAME_INFO -> 2nd index
                //PacketPlayOutChat ppoc = new PacketPlayOutChat(icbc, ChatMessageType.GAME_INFO, p.getUniqueId());
                charMessageTypeEnum = chatMessageTypeClass.getEnumConstants()[2];
                //1.16 - ?.?? : PacketPlayOutChat(IChatBaseComponent, ChatMessageType, UUID)
                packetPlayOutChatConstructor = packetPlayOutChatClass
                        .getConstructor(iChatBaseComponentClass, chatMessageTypeClass, UUID.class);
            }

            Util.printToTerminal("Checkpoint 6");

        } catch (Exception e) { //ReflectionError
            Util.printToTerminal(Util.ERR + "Exception while initializing packets with NMS version 1."
                    + versionStr + ". Version may be incompatible.");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Sends a message to the player's actionbar using reflected methods.
     * @param p Recipient player.
     * @param msg Message to send.
     */
    private static void sendToActionBar(Player p, String msg) {

        // Use spigot API if possible
        if (isSpigot) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
            return;
        }

        try {
            p.getClass().getMethod("getHandle").invoke(p);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Use NMS otherwise
        try {
            //IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + msg + "\"}");
            Object icbc = chatMessageConstructor.newInstance(msg, new Object[0]);

            //PacketPlayOutChat ppoc =
            Object packet;
            //1.8 - 1.11
            if (Util.apiVersion < 12) {
                //= new PacketPlayOutChat(icbc, byte);
                packet = packetPlayOutChatConstructor.newInstance(icbc, (byte) 2);
            }
            //1.12 - 1.16
            else if (Util.apiVersion < 16) {
                //= new PacketPlayOutChat(icbc, ChatMessageType.GAME_INFO);
                packet = packetPlayOutChatConstructor.newInstance(icbc, charMessageTypeEnum);
            }
            //1.16 - ?.?? TODO check if changed each update
            else {
                //= new PacketPlayOutChat(icbc, ChatMessageType.GAME_INFO, UUID);
                packet = packetPlayOutChatConstructor.newInstance(icbc, charMessageTypeEnum, p.getUniqueId());
            }
            /* CraftPlayer p = (CraftPlayer) player; */
            //Object craftPlayer = CraftPlayer_CLASS.cast(p);
            /* p.getHandle(); */
            //Object playerHandle = getHandle_MET.invoke(craftPlayer);
            /* p.getHandle().playerConnection; */
            //Object playerConnection = playerConnection_FIELD.get(playerHandle);
            /* p.getHandle().playerConnection.sendPacket(ppoc); */
            //sendPacket_MET.invoke(playerConnection, packet);

            sendPacketMethod.invoke(playerConnectionField.get(
                    getHandleMethod.invoke(craftPlayerClass.cast(p))), packet);


        } catch (Exception e) {
            Util.printToTerminal("Fatal error while sending packets. Shutting down...");
            Bukkit.getPluginManager().disablePlugin(instance);
            e.printStackTrace();
        }
    }

}
