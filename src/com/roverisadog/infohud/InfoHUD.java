
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

    //Take all class creation, method call, field call from usual actionbar sendmessage
    private Class<?> CraftPlayer_CLASS, PacketPlayOutChat_CLASS, IChatBaseComponent_CLASS, ChatMessage_CLASS, Packet_CLASS, ChatMessageType_CLASS;
    private Field playerConnection_FIELD;
    private Method getHandle_MET, sendPacket_MET;
    private Constructor<?> PacketPlayOutChat_CONST, ChatMessage_CONST;
    private Object CHATMESSAGETYPE_ENUM;

    private String versionStr;
    private long bmStart;
    private long bmEnd;


    @Override
    public void onEnable() {
        try {
            Util.print(Util.GREN + "InfoHUD Enabling...");

            //Save initial cfg or load.
            this.saveDefaultConfig(); //Silent fails if config.yml already exists
            if (!Util.loadConfig(this)) {
                throw new Exception(Util.ERROR + "Error while reading config.yml. Shutting down...");
            }

            //Version check and reflection attempt
            this.versionStr = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3]; //Eg: org.bukkit.craftbukkit.v1_16_R2

            Util.apiVersion = Integer.parseInt(versionStr.split("_")[1]);
            /*
            if (Integer.parseInt(versionStr.split("_")[1]) < 12)
                Util.versionInt = 0;
            else if (Integer.parseInt(versionStr.split("_")[1]) < 16)
                Util.versionInt = 1;
            else
                Util.versionInt = 2;
            */
            Util.print(Util.GREN + "API Version: " + Util.HIGHLIGHT + Util.apiVersion);

            if (!isCompatibleVersion() || !reflectionPackets()){
                throw new Exception(Util.ERROR + "Version error. Shutting down...");
            }

            Objects.requireNonNull(this.getCommand(Util.CMD_NAME)).setExecutor(new CommandHandler());

            Util.task = start(this);
            Util.print(Util.GREN + "InfoHUD Successfully Enabled.");
        }
        catch (Exception e){
            Util.print(e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    /** Uses reflection to init required classes regardless of version. Prone to failure. */
    private boolean reflectionPackets(){
        /* VERSION SPECIFIC PSEUDOCODE
        static void sendToActionBar(Player player, String msg){
        CraftPlayer p = (CraftPlayer) player;
        IChatBaseComponent icbc <- IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + msg + "\"}");
        PacketPlayOutChat ppoc <- new PacketPlayOutChat(icbc, ChatMessageType.GAME_INFO, UUID);
        p.getHandle().playerConnection.sendPacket(ppoc);
         */
        try {
            //org.bukkit.craftbukkit.VERSION.entity.CraftPlayer; | CraftPlayer p = (CraftPlayer) player;
            CraftPlayer_CLASS = Class.forName("org.bukkit.craftbukkit." + versionStr + ".entity.CraftPlayer");
            IChatBaseComponent_CLASS = Class.forName("net.minecraft.server." + versionStr + ".IChatBaseComponent"); //import net.minecraft.server.v1_16_R2.IChatBaseComponent;

            //Used to get Player.sendPackets -> p.getHandle().playerConnection.sendPacket(ppoc); PPOC is instance of packet.
            Packet_CLASS = Class.forName("net.minecraft.server." + versionStr + ".Packet");

            //Get methods and fields from sending packet line -> //p.getHandle().playerConnection.sendPacket(ppoc);
            getHandle_MET = CraftPlayer_CLASS.getMethod("getHandle");
            playerConnection_FIELD = getHandle_MET.getReturnType().getField("playerConnection");
            sendPacket_MET = playerConnection_FIELD.getType().getMethod("sendPacket", Packet_CLASS);

            //IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + msg + "\"}");
            ChatMessage_CLASS = Class.forName("net.minecraft.server." + versionStr + ".ChatMessage");
            ChatMessage_CONST = ChatMessage_CLASS.getConstructor(String.class, Object[].class);

            //PacketPlayOutChat ppoc = new PacketPlayOutChat(icbc, ChatMessageType.GAME_INFO, p.getUniqueId());
            PacketPlayOutChat_CLASS = Class.forName("net.minecraft.server." + versionStr + ".PacketPlayOutChat"); //import net.minecraft.server.VERSION.IChatBaseComponent;

            if (Util.apiVersion < 12){ //1.8 - 1.11
                //1.8 - 1.11 : PacketPlayOutChat(IChatBaseComponent, byte)
                PacketPlayOutChat_CONST = PacketPlayOutChat_CLASS.getConstructor(IChatBaseComponent_CLASS, byte.class);
            }
            else if (Util.apiVersion < 16){ //1.12 - 1.15
                //import net.minecraft.server.v1_16_R2.ChatMessageType;
                ChatMessageType_CLASS = Class.forName("net.minecraft.server." + versionStr + ".ChatMessageType"); //Nonexistant on 1.8
                //ChatMessageType.GAME_INFO -> 2 | PacketPlayOutChat ppoc = new PacketPlayOutChat(icbc, ChatMessageType.GAME_INFO, p.getUniqueId());
                CHATMESSAGETYPE_ENUM = ChatMessageType_CLASS.getEnumConstants()[2];
                //1.12 - 1.16 : PacketPlayOutChat(IChatBaseComponent, ChatMessageType)
                PacketPlayOutChat_CONST = PacketPlayOutChat_CLASS.getConstructor(IChatBaseComponent_CLASS, ChatMessageType_CLASS);
            }
            else { //1.16+
                //import net.minecraft.server.v1_16_R2.ChatMessageType;
                ChatMessageType_CLASS = Class.forName("net.minecraft.server." + versionStr + ".ChatMessageType"); //Nonexistant on 1.8
                //ChatMessageType.GAME_INFO -> 2 | PacketPlayOutChat ppoc = new PacketPlayOutChat(icbc, ChatMessageType.GAME_INFO, p.getUniqueId());
                CHATMESSAGETYPE_ENUM = ChatMessageType_CLASS.getEnumConstants()[2];
                //1.16 - ?.?? : PacketPlayOutChat(IChatBaseComponent, ChatMessageType, UUID)
                PacketPlayOutChat_CONST = PacketPlayOutChat_CLASS.getConstructor(IChatBaseComponent_CLASS, ChatMessageType_CLASS, UUID.class);
            }

        } catch (Exception e) {
            Util.print(Util.ERROR + "Exception while initializing packets with version " + versionStr + ". Version may be incompatible.");
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return false;
        }
        return true;
    }

    private void sendToActionBar(Player p, String msg) {
        /* VERSION SPECIFIC PSEUDOCODE
        static void sendToActionBar(Player player, String msg){
        CraftPlayer p = (CraftPlayer) player;
        IChatBaseComponent icbc <- IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + msg + "\"}");
        PacketPlayOutChat ppoc <- new PacketPlayOutChat(icbc, ChatMessageType.GAME_INFO, UUID);
        p.getHandle().playerConnection.sendPacket(ppoc);
         */
        try {
            //IChatBaseComponent icbc <- IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + msg + "\"}");
            Object icbc = ChatMessage_CONST.newInstance(msg, new Object[0]);

            //PacketPlayOutChat ppoc <- new PacketPlayOutChat(icbc, ChatMessageType.GAME_INFO, UUID);
            Object packet;
            if (Util.apiVersion < 12) {
                packet = PacketPlayOutChat_CONST.newInstance(icbc, (byte) 2);
            }
            else if (Util.apiVersion < 16) { //1.12 - 1.16
                packet = PacketPlayOutChat_CONST.newInstance(icbc, CHATMESSAGETYPE_ENUM);
            }
            else { //1.16+
                packet = PacketPlayOutChat_CONST.newInstance(icbc, CHATMESSAGETYPE_ENUM, p.getUniqueId());
            }
            //CraftPlayer p = (CraftPlayer) player;
            Object craftPlayer = CraftPlayer_CLASS.cast(p);
            //p.getHandle()
            Object playerHandle = getHandle_MET.invoke(craftPlayer);
            //p.getHandle().playerConnection
            Object playerConnection = playerConnection_FIELD.get(playerHandle);
            //p.getHandle().playerConnection.sendPacket(ppoc);
            sendPacket_MET.invoke(playerConnection, packet);


        } catch (Exception e){
            //Don't do anything
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        Util.print(Util.GREN + "InfoHUD Disabled");
    }

    public BukkitTask start(final Plugin plugin) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {

            public void run(){
                bmStart = System.nanoTime();
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    //Skip players that are not on the list
                    if (!Util.isEnabled(p)) continue;
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

                    if (darkMode == 1){ //enabled : 1
                        col1 = Util.DBLU;
                        col2 = Util.DAQA;
                    }
                    else if (darkMode == 2){ //auto : 2
                        boolean bright = Util.isInBrightBiome(p);
                        col1 = bright ? Util.DBLU : Util.GLD;
                        col2 = bright ? Util.DAQA : Util.WHI;
                    }

                    //Coordinates enabled
                    if (coordsMode == 1){
                        //Only display coords
                        if (timeMode == 0){
                            sendToActionBar(p, col1 + "XYZ: " + col2 + Util.getCoordinates(p));
                        }
                        //Display coords and time in ticks
                        else if (timeMode == 1){
                            sendToActionBar(p,
                                    col1 + "XYZ: " + col2 + Util.getCoordinates(p) + " " +
                                            col1 + String.format("%-10s", Util.getPlayerDirection(p)) + col2 + p.getWorld().getTime());
                        }
                        //Display coords and time in HH:mm
                        else if (timeMode == 2){
                            sendToActionBar(p,
                                    col1 + "XYZ: " + col2 + Util.getCoordinates(p) + " " +
                                            col1 + String.format("%-10s", Util.getPlayerDirection(p)) + col2 + Util.getTime24(p.getWorld().getTime()));
                        }
                        //Display coords and villager timer
                        else if (timeMode == 3){
                            sendToActionBar(p,
                                    col1 + "XYZ: " + col2 + Util.getCoordinates(p) + " " +
                                            col1 + String.format("%-10s", Util.getPlayerDirection(p)) + Util.getVillagerTimeLeft(p.getWorld().getTime(), col1, col2));
                        }
                    }

                    //Coordinates disabled
                    else if (coordsMode == 0){
                        //Display time in ticks
                        if (timeMode == 1){
                            sendToActionBar(p, col2 + p.getWorld().getTime());
                        }
                        //Display time in HH:mm
                        else if (timeMode == 2){
                            sendToActionBar(p, col2 + Util.getTime24(p.getWorld().getTime()));
                        }
                        //Display villager timer
                        else if (timeMode == 3){
                            sendToActionBar(p, col2 + Util.getVillagerTimeLeft(p.getWorld().getTime(), col1, col2));
                        }
                    }
                    Util.benchmark = System.nanoTime() - bmStart;
                }
            }
        }, 0L, Util.getRefreshRate());
    }

    @Deprecated
    private boolean isCompatibleVersion() {
        /*
        String version;
        try {
            version = Bukkit.getServer().getClass().getPackage().getName().split(".")[3]; //org.bukkit.craftbukkit.version.blabla
        } catch (Exception e) {
            return false;
        }
        if (version.equalsIgnoreCase("v1_16_R2"))
        */
        return true;
    }

}
