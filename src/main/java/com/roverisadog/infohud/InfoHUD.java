
package com.roverisadog.infohud;

import com.roverisadog.infohud.command.CoordMode;
import com.roverisadog.infohud.command.DarkMode;
import com.roverisadog.infohud.command.TimeMode;
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


            Util.printToTerminal(Util.GRN + "InfoHUD Successfully Enabled on " + Util.WHI + "NMS Version 1." + Util.apiVersion);
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
        /* VERSION SPECIFIC PSEUDOCODE
        static void sendToActionBar(Player player, String msg) {
            CraftPlayer p = (CraftPlayer) player;
            IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + msg + "\"}");
            PacketPlayOutChat ppoc = new PacketPlayOutChat(icbc, ChatMessageType.GAME_INFO, UUID);
            p.getHandle().playerConnection.sendPacket(ppoc);
        }
         */
        try {
            //org.bukkit.craftbukkit.VERSION.entity.CraftPlayer; | CraftPlayer p = (CraftPlayer) player;
            craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + versionStr + ".entity.CraftPlayer");
            //import net.minecraft.server.v1_16_R2.IChatBaseComponent;
            Class<?> iChatBaseComponentClass = Class
                    .forName("net.minecraft.server." + versionStr + ".IChatBaseComponent");

            //Used to get Player.sendPackets -> p.getHandle().playerConnection.sendPacket(ppoc);
            //PPOC is instance of packet.
            Class<?> packetClass = Class.forName("net.minecraft.server." + versionStr + ".Packet");

            //Get methods and fields from sending packet line -> //p.getHandle().playerConnection.sendPacket(ppoc);
            getHandleMethod = craftPlayerClass.getMethod("getHandle");
            playerConnectionField = getHandleMethod.getReturnType().getField("playerConnection");
            sendPacketMethod = playerConnectionField.getType().getMethod("sendPacket", packetClass);

            //IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + msg + "\"}");
            Class<?> chatMessageClass = Class.forName("net.minecraft.server." + versionStr + ".ChatMessage");
            chatMessageConstructor = chatMessageClass.getConstructor(String.class, Object[].class);

            //import net.minecraft.server.VERSION.IChatBaseComponent;
            //PacketPlayOutChat ppoc = new PacketPlayOutChat(icbc, ChatMessageType.GAME_INFO, p.getUniqueId());
            Class<?> packetPlayOutChatClass = Class
                    .forName("net.minecraft.server." + versionStr + ".PacketPlayOutChat");

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
                        .forName("net.minecraft.server." + versionStr + ".ChatMessageType"); //Nonexistent on 1.8
                // ChatMessageType.GAME_INFO -> 2
                // PacketPlayOutChat ppoc = new PacketPlayOutChat(icbc, ChatMessageType.GAME_INFO, p.getUniqueId());
                charMessageTypeEnum = chatMessageTypeClass.getEnumConstants()[2];
                //1.12 - 1.16 : PacketPlayOutChat(IChatBaseComponent, ChatMessageType)
                packetPlayOutChatConstructor = packetPlayOutChatClass
                        .getConstructor(iChatBaseComponentClass, chatMessageTypeClass);
            }
            //1.16 - ?.?? TODO check if changed each update
            else {
                //import net.minecraft.server.v1_16_R2.ChatMessageType;
                chatMessageTypeClass = Class
                        .forName("net.minecraft.server." + versionStr + ".ChatMessageType"); //Nonexistent on 1.8
                //ChatMessageType.GAME_INFO -> 2nd index
                //PacketPlayOutChat ppoc = new PacketPlayOutChat(icbc, ChatMessageType.GAME_INFO, p.getUniqueId());
                charMessageTypeEnum = chatMessageTypeClass.getEnumConstants()[2];
                //1.16 - ?.?? : PacketPlayOutChat(IChatBaseComponent, ChatMessageType, UUID)
                packetPlayOutChatConstructor = packetPlayOutChatClass
                        .getConstructor(iChatBaseComponentClass, chatMessageTypeClass, UUID.class);
            }

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
