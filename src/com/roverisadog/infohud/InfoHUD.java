
package com.roverisadog.infohud;

import com.roverisadog.infohud.command.CoordMode;
import com.roverisadog.infohud.command.DarkMode;
import com.roverisadog.infohud.command.TimeMode;
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

    //Reflected objects for NMS packets.
    private Class<?> craftPlayerClass;
    private Field playerConnectionField;
    private Method getHandleMethod, sendPacketMethod;
    private Constructor<?> packetPlayOutChatConstructor, chatMessageConstructor;
    private Object charMessageTypeEnum;

    private String versionStr;

    /** Time elapsed for the last update. */
    private long benchmarkStart;
    /**  */
    private int lowFreqTimer;

    protected BukkitTask task;

    /**
     * Initial setup:
     * Load config, get version, get NMS packet classes.
     * Configure CommandHandler
     */
    @Override
    public void onEnable() {
        try {
            Util.plugin = this;
            Util.print(Util.GRN + "InfoHUD Enabling...");

            //Save initial cfg or load.
            this.saveDefaultConfig(); //Silent fails if config.yml already exists
            if (!Util.loadConfig(this)) {
                throw new Exception(Util.ERR + "Error while reading config.yml.");
            }

            //Version check
            //Eg: org.bukkit.craftbukkit.v1_16_R2.blabla
            String ver = Bukkit.getServer().getClass().getPackage().getName();
            this.versionStr = ver.split("\\.")[3];
            Util.apiVersion = Integer.parseInt(versionStr.split("_")[1]);
            Util.serverVendor = ver.split("\\.")[2];

            //Attempt to get version-specific NMS packets class.
            if (!reflectionPackets()) {
                throw new Exception(Util.ERR + "Version error.");
            }

            //Setup command
            Objects.requireNonNull(this.getCommand(Util.CMD_NAME)).setExecutor(new CommandExecutor(this));

            task = start(this);
            Util.print(Util.GRN + "InfoHUD Successfully Enabled on " + Util.WHI + "NMS Version 1." + Util.apiVersion);
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
            Util.print(Util.ERR + "Exception while initializing packets with NMS version 1."
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
    private void sendToActionBar(Player p, String msg) {
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
            Util.print("Fatal error while sending packets. Shutting down...");
            Bukkit.getPluginManager().disablePlugin(this);
            e.printStackTrace();
        }
    }

    /** Clean up while shutting down (Currently nothing). */
    @Override
    public void onDisable() {
        Util.print(Util.GRN + "InfoHUD Disabled");
    }

    /**
     * Principal task to be given to the bukkit scheduler.
     * @param plugin This.
     * @return Task created.
     */
    public BukkitTask start(Plugin plugin) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            benchmarkStart = System.nanoTime();
            lowFreqTimer++;
            for (Player p : plugin.getServer().getOnlinePlayers()) {

                //Skip players that are not on the list
                if (!PlayerCfg.isEnabled(p)) {
                    continue;
                }

                //Assumes that online players << saved players

                PlayerCfg cfg = PlayerCfg.getConfig(p);

                //CoordMode coordMode = Util.getCoordinatesMode(p);
                //TimeMode timeMode = Util.getTimeMode(p);
                //DarkMode darkMode = Util.getDarkMode(p);

                if (cfg.coordMode == CoordMode.DISABLED && cfg.timeMode == TimeMode.DISABLED) {
                    PlayerCfg.removePlayer(p);
                    continue;
                }

                //Setting dark mode colors -> Assume disabled : 0
                String col1; //Text
                String col2; //Values

                if (cfg.darkMode == DarkMode.AUTO) {
                    if (cfg.isInBrightBiome) {
                        col1 = Util.Dark1;
                        col2 = Util.Dark2;
                    }
                    else {
                        col1 = Util.Bright1;
                        col2 = Util.Bright2;
                    }
                }
                else if (cfg.darkMode == DarkMode.DISABLED) {
                    col1 = Util.Bright1;
                    col2 = Util.Bright2;
                }
                else { //Enabled
                    col1 = Util.Dark1;
                    col2 = Util.Dark2;
                }


                if (cfg.coordMode == CoordMode.ENABLED) {
                    switch (cfg.timeMode) {
                        //Only display coords
                        case DISABLED:
                            sendToActionBar(p, col1 + "XYZ: " + col2 + cfg.coordMode.toString(p, col1, col2));
                            break;
                        case CURRENT_TICK:
                        case CLOCK24:
                        case CLOCK12:
                            sendToActionBar(p, col1 + "XYZ: "
                                    + col2 + cfg.coordMode.toString(p, col1, col2) + " "
                                    + col1 + String.format("%-10s", Util.getPlayerDirection(p))
                                    + col2 + cfg.timeMode.toString(p, col1, col2));
                            break;
                        default:
                    }
                }

                //Coordinates disabled
                else if (cfg.coordMode == CoordMode.DISABLED) {
                    sendToActionBar(p, col2 + cfg.timeMode.toString(p, col1, col2));
                }
            }
            if (lowFreqTimer > 40) {
                lowFreqTimer = 0;
                runLowFreqTasks(plugin);
            }
            Util.benchmark = System.nanoTime() - benchmarkStart;
        }, 0L, Util.getRefreshPeriod());
    }

    /**
     * Runs expensive tasks in a new thread (for now, biome fetching).
     */
    private void runLowFreqTasks(Plugin plugin) {
        new Thread(() -> {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (PlayerCfg.getConfig(p).darkMode == DarkMode.AUTO) {
                    Util.updateIsInBrightBiome(p);
                }
            }
        }).start();
    }
}
