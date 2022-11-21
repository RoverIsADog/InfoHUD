package com.roverisadog.infohud.message;

import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * NMS for sending messages to the actionbar, from 1.8 to 1.11 inclusively
 */
public class ActionBarSenderNMS1_8 implements ActionBarSender {

	private static Class<?> craftPlayerClass;
	private static Method getHandleMethod;
	private static Field playerConnectionField;
	private static Method sendPacketMethod;
	private static Constructor<?> packetPlayOutChatConstructor;
	private static Constructor<?> chatMessageConstructor;

	/**
	 * Initialize NMS if applicable.
	 * @param versionStr Internal string for the server version.
	 * @throws Exception Reflection errors or otherwise.
	 */
	public ActionBarSenderNMS1_8(String versionStr) throws Exception {
		String nmsPath = "net.minecraft.server." + versionStr + ".";

		//org.bukkit.craftbukkit.VERSION.entity.CraftPlayer; | CraftPlayer p = (CraftPlayer) player;
		craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + versionStr + ".entity.CraftPlayer");

		//import net.minecraft.server.v1_16_R2.IChatBaseComponent;
		Class<?> iChatBaseComponentClass = Class.forName(nmsPath + "IChatBaseComponent");

		//import net.minecraft.server.v1_16_R2.Packet
		Class<?> packetClass = Class.forName(nmsPath + "Packet");

		// p.getHandle().playerConnection.sendPacket(ppoc);
		getHandleMethod = craftPlayerClass.getMethod("getHandle");
		playerConnectionField = getHandleMethod.getReturnType().getField("playerConnection");
		sendPacketMethod = playerConnectionField.getType().getMethod("sendPacket", packetClass);

		// IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + msg + "\"}");
		Class<?> chatMessageClass = Class.forName(nmsPath + "ChatMessage");
		chatMessageConstructor = chatMessageClass.getConstructor(String.class, Object[].class);

		// import net.minecraft.server.VERSION.IChatBaseComponent;
		Class<?> packetPlayOutChatClass = Class.forName(nmsPath + "PacketPlayOutChat");
		// PacketPlayOutChat ppoc = new PacketPlayOutChat(icbc, (byte) 2);
		packetPlayOutChatConstructor = packetPlayOutChatClass
				.getConstructor(iChatBaseComponentClass, byte.class);

	}

	@Override
	public void sendToActionBar(Player p, String msg) throws Exception {
		//IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + msg + "\"}");
		Object icbc = chatMessageConstructor.newInstance(msg, new Object[0]);
		Object packet = packetPlayOutChatConstructor.newInstance(icbc, (byte) 2);

		// CraftPlayer p = (CraftPlayer) player;
		Object cp = craftPlayerClass.cast(p);
		// p.getHandle().playerConnection.sendPacket(ppoc);
		sendPacketMethod.invoke(playerConnectionField.get(
				getHandleMethod.invoke(cp)), packet);
	}
}
