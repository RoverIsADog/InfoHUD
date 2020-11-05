
package com.roverisadog.infohud;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;

public class InfoHUD extends JavaPlugin {

    //import net.minecraft.server.v1_16_R2.ChatMessageType;
    //import net.minecraft.server.v1_16_R2.IChatBaseComponent;
    //import net.minecraft.server.v1_16_R2.PacketPlayOutChat;
    //import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;

    //Reflected classes for NMS packets.
    private Class<?> CraftPlayer_CLASS;
    private Field playerConnection_FIELD;
    private Method getHandle_MET, sendPacket_MET;
    private Constructor<?> PacketPlayOutChat_CONST, ChatMessage_CONST;
    private Object CHATMESSAGETYPE_ENUM;

    private String versionStr;
    private long bmStart;

    BukkitTask task;

    /**
     * Initial setup:
     * Load config, get version, get NMS packet classes.
     * Configure CommandHandler
     */
    @Override
    public void onEnable() {
        try {
            Util.plugin = this;
            Util.print(Util.GREN + "InfoHUD Enabling...");

            //Save initial cfg or load.
            this.saveDefaultConfig(); //Silent fails if config.yml already exists
            if (!Util.loadConfig(this)) {
                throw new Exception(Util.ERR + "Error while reading config.yml.");
            }

            //Version check
            String temp = Bukkit.getServer().getClass().getPackage().getName(); //Eg: org.bukkit.craftbukkit.v1_16_R2.blabla
            this.versionStr = temp.split("\\.")[3];
            Util.apiVersion = Integer.parseInt(versionStr.split("_")[1]);
            Util.serverVendor = temp.split("\\.")[2];

            //Attempt to get version-specific NMS packets class.
            if (!reflectionPackets()) {
                throw new Exception(Util.ERR + "Version error.");
            }

            //Setup command
            Objects.requireNonNull(this.getCommand(Util.CMD_NAME)).setExecutor(new CommandHandler(this));

            task = start(this);
            Util.print(Util.GREN + "InfoHUD Successfully Enabled. " + Util.WHI + "NMS Version Detected: 1." + Util.apiVersion);
        }
        catch (Exception e) {
            Util.print(e.getMessage());
            Util.print("Shutting down...");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    /**
     *  Uses reflection to get version specific NMS packet-related classes.
     *  Preferred to Spigot built in method for wider compatibility.
     */
    private boolean reflectionPackets() {
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
            CraftPlayer_CLASS = Class.forName("org.bukkit.craftbukkit." + versionStr + ".entity.CraftPlayer");
            //import net.minecraft.server.v1_16_R2.IChatBaseComponent;
            Class<?> IChatBaseComponent_CLASS = Class.forName("net.minecraft.server." + versionStr + ".IChatBaseComponent");

            //Used to get Player.sendPackets -> p.getHandle().playerConnection.sendPacket(ppoc); PPOC is instance of packet.
            Class<?> packet_CLASS = Class.forName("net.minecraft.server." + versionStr + ".Packet");

            //Get methods and fields from sending packet line -> //p.getHandle().playerConnection.sendPacket(ppoc);
            getHandle_MET = CraftPlayer_CLASS.getMethod("getHandle");
            playerConnection_FIELD = getHandle_MET.getReturnType().getField("playerConnection");
            sendPacket_MET = playerConnection_FIELD.getType().getMethod("sendPacket", packet_CLASS);

            //IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + msg + "\"}");
            Class<?> chatMessage_CLASS = Class.forName("net.minecraft.server." + versionStr + ".ChatMessage");
            ChatMessage_CONST = chatMessage_CLASS.getConstructor(String.class, Object[].class);

            //import net.minecraft.server.VERSION.IChatBaseComponent;
            //PacketPlayOutChat ppoc = new PacketPlayOutChat(icbc, ChatMessageType.GAME_INFO, p.getUniqueId());
            Class<?> packetPlayOutChat_CLASS = Class.forName("net.minecraft.server." + versionStr + ".PacketPlayOutChat");

            Class<?> chatMessageType_CLASS;
            if (Util.apiVersion < 12) { //1.8 - 1.11
                //1.8 - 1.11 : PacketPlayOutChat(IChatBaseComponent, byte)
                PacketPlayOutChat_CONST = packetPlayOutChat_CLASS.getConstructor(IChatBaseComponent_CLASS, byte.class);
            }
            else if (Util.apiVersion < 16) { //1.12 - 1.15
                //import net.minecraft.server.v1_16_R2.ChatMessageType;
                chatMessageType_CLASS = Class.forName("net.minecraft.server." + versionStr + ".ChatMessageType"); //Nonexistent on 1.8
                //ChatMessageType.GAME_INFO -> 2 | PacketPlayOutChat ppoc = new PacketPlayOutChat(icbc, ChatMessageType.GAME_INFO, p.getUniqueId());
                CHATMESSAGETYPE_ENUM = chatMessageType_CLASS.getEnumConstants()[2];
                //1.12 - 1.16 : PacketPlayOutChat(IChatBaseComponent, ChatMessageType)
                PacketPlayOutChat_CONST = packetPlayOutChat_CLASS.getConstructor(IChatBaseComponent_CLASS, chatMessageType_CLASS);
            }
            else { //1.16 - ?.?? TODO check if changed each update
                //import net.minecraft.server.v1_16_R2.ChatMessageType;
                chatMessageType_CLASS = Class.forName("net.minecraft.server." + versionStr + ".ChatMessageType"); //Nonexistent on 1.8
                //ChatMessageType.GAME_INFO -> 2 | PacketPlayOutChat ppoc = new PacketPlayOutChat(icbc, ChatMessageType.GAME_INFO, p.getUniqueId());
                CHATMESSAGETYPE_ENUM = chatMessageType_CLASS.getEnumConstants()[2];
                //1.16 - ?.?? : PacketPlayOutChat(IChatBaseComponent, ChatMessageType, UUID)
                PacketPlayOutChat_CONST = packetPlayOutChat_CLASS.getConstructor(IChatBaseComponent_CLASS, chatMessageType_CLASS, UUID.class);
            }

        } catch (Exception e) { //ReflectionError
            Util.print(Util.ERR + "Exception while initializing packets with NMS version 1." + versionStr + ". Version may be incompatible.");
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
    private void sendToActionBar(Player p, String msg) {
        try {
            //IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + msg + "\"}");
            Object icbc = ChatMessage_CONST.newInstance(msg, new Object[0]);

            //PacketPlayOutChat ppoc = new PacketPlayOutChat(icbc, ChatMessageType.GAME_INFO, UUID);
            Object packet;
            if (Util.apiVersion < 12) {
                packet = PacketPlayOutChat_CONST.newInstance(icbc, (byte) 2);
            }
            else if (Util.apiVersion < 16) { //1.12 - 1.16
                packet = PacketPlayOutChat_CONST.newInstance(icbc, CHATMESSAGETYPE_ENUM);
            }
            else { //1.16 - ?.?? TODO check if changed each update
                packet = PacketPlayOutChat_CONST.newInstance(icbc, CHATMESSAGETYPE_ENUM, p.getUniqueId());
            }
            /* CraftPlayer p = (CraftPlayer) player; */
            //Object craftPlayer = CraftPlayer_CLASS.cast(p);
            /* p.getHandle(); */
            //Object playerHandle = getHandle_MET.invoke(craftPlayer);
            /* p.getHandle().playerConnection; */
            //Object playerConnection = playerConnection_FIELD.get(playerHandle);
            /* p.getHandle().playerConnection.sendPacket(ppoc); */
            //sendPacket_MET.invoke(playerConnection, packet);

            sendPacket_MET.invoke(playerConnection_FIELD.get(
                    getHandle_MET.invoke(CraftPlayer_CLASS.cast(p))), packet);


        } catch (Exception e) {
            Util.print("Fatal error while sending packets. Shutting down...");
            Bukkit.getPluginManager().disablePlugin(this);
            e.printStackTrace();
        }
    }

    /** Clean up while shutting down (Currently nothing). */
    @Override
    public void onDisable() {
        Util.print(Util.GREN + "InfoHUD Disabled");
    }

    /**
     * Principal task to be given to the bukkit scheduler.
     * @param plugin This.
     * @return Task created.
     */
    public BukkitTask start(final Plugin plugin) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            bmStart = System.nanoTime();
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                //Skip players that are not on the list
                if (!Util.isEnabled(p)) {
                    continue;
                }
                /* Assumes that online players << saved players */

                int coordsMode = Util.getCoordinatesMode(p);
                int timeMode = Util.getTimeMode(p);
                int darkMode = Util.getDarkMode(p);

                if (coordsMode == 0 && timeMode == 0) {
                    Util.removePlayer(p);
                    continue;
                }

                //Setting dark mode colors -> Assume disabled : 0
                String col1 = Util.GLD; //Text
                String col2 = Util.WHI; //Values

                if (darkMode == 1) { //enabled : 1
                    col1 = Util.DBLU;
                    col2 = Util.DAQA;
                }
                else if (darkMode == 2) { //auto : 2
                    boolean bright = Util.isInBrightBiome(p);
                    col1 = bright ? Util.DBLU : Util.GLD;
                    col2 = bright ? Util.DAQA : Util.WHI;
                }

                if (coordsMode == 1) {
                    switch (timeMode) {
                        //Only display coords
                        case 0:
                            sendToActionBar(p, col1 + "XYZ: " + col2 + Util.getCoordinatesStr(p));
                            break;
                        //Display coords and time in ticks
                        case 1:
                            sendToActionBar(p, col1 + "XYZ: " + col2 + Util.getCoordinatesStr(p) + " " +
                                col1 + String.format("%-10s", Util.getPlayerDirection(p)) + col2 + p.getWorld().getTime());
                            break;
                        //Display coords and time in HH:mm
                        case 2:
                            sendToActionBar(p, col1 + "XYZ: " + col2 + Util.getCoordinatesStr(p) + " " +
                                col1 + String.format("%-10s", Util.getPlayerDirection(p)) + col2 + Util.getTime24(p.getWorld().getTime()));
                            break;
                        //Display coords and villager timer
                        case 3:
                            sendToActionBar(p, col1 + "XYZ: " + col2 + Util.getCoordinatesStr(p) + " " +
                                col1 + String.format("%-10s", Util.getPlayerDirection(p)) + Util.getVillagerTimeLeft(p.getWorld().getTime(), col1, col2));
                        default:

                    }
                }

                //Coordinates disabled
                else if (coordsMode == 0) {
                    switch (timeMode) {
                        //Display time in ticks
                        case 1:
                            sendToActionBar(p, col2 + p.getWorld().getTime());
                            break;
                        //Display time in HH:mm
                        case 2:
                            sendToActionBar(p, col2 + Util.getTime24(p.getWorld().getTime()));
                            break;
                        //Display villager timer
                        case 3:
                            sendToActionBar(p, col2 + Util.getVillagerTimeLeft(p.getWorld().getTime(), col1, col2));
                            break;
                        default:
                    }
                }
            }
            Util.benchmark = System.nanoTime() - bmStart;
        }, 0L, Util.getRefreshRate());
    }

}
