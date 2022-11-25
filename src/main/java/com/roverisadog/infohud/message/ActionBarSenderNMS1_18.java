package com.roverisadog.infohud.message;

import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * NMS for sending messages to the actionbar, for 1.18
 */
public class ActionBarSenderNMS1_18 implements ActionBarSender {

	private static Class<?> playerClass;
	private static Method getHandleMethod;
	private static Field playerConnectionField;
	private static Method sendPacketMethod;
	private static Constructor<?> packetPlayOutChatConstructor;
	private static Constructor<?> chatMessageConstructor;
	private static Object chatMessageTypeEnum;

	/**
	 * Initialize NMS if applicable.
	 * @param versionStr Internal string for the server version.
	 * @throws Exception Reflection errors or otherwise.
	 */
	public ActionBarSenderNMS1_18(String versionStr) throws Exception {
		String nmsPath = "net.minecraft.";

		/* --------------------------------- Player related --------------------------------- */

		//org.bukkit.craftbukkit.entity.Player; | Player p = player; // Casting no longer necessary
		playerClass = Class.forName("org.bukkit.craftbukkit." + versionStr + ".entity.CraftPlayer");

		//import net.minecraft.server.v1_16_R2.IChatBaseComponent;
		Class<?> iChatBaseComponentClass = Class.forName(nmsPath + "network.chat.IChatBaseComponent"); //

		//import net.minecraft.server.v1_16_R2.Packet
		Class<?> packetClass = Class.forName(nmsPath + "network.protocol.Packet");

		// p.getHandle().playerConnection.sendPacket(ppoc);
		getHandleMethod = playerClass.getMethod("getHandle");
		playerConnectionField = getHandleMethod.getReturnType().getField("b"); // playerConnection obfuscated to b
		sendPacketMethod = playerConnectionField.getType().getMethod("a", packetClass); // sendPacket obfuscated to a

		// IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + msg + "\"}");
		Class<?> chatMessageClass = Class.forName(nmsPath + "network.chat.ChatMessage");
		chatMessageConstructor = chatMessageClass.getConstructor(String.class, Object[].class);

		// import net.minecraft.server.VERSION.IChatBaseComponent;
		Class<?> packetPlayOutChatClass = Class.forName(nmsPath+ "network.protocol.game.PacketPlayOutChat"); //

		// import net.minecraft.server.v1_16_R2.ChatMessageType;
		Class<?> chatMessageTypeClass = Class.forName(nmsPath + "network.chat.ChatMessageType"); //
		// ChatMessageType.GAME_INFO: 2nd index of enum
		chatMessageTypeEnum = chatMessageTypeClass.getEnumConstants()[2];
		// PacketPlayOutChat ppoc = new PacketPlayOutChat(icbc, ChatMessageType.GAME_INFO);
		packetPlayOutChatConstructor = packetPlayOutChatClass
				.getConstructor(iChatBaseComponentClass, chatMessageTypeClass, UUID.class);

	}

	/**
	 * NMS has code refactoring and obfuscation. Otherwise, same as {@link ActionBarSenderNMS1_16}.
	 */
	@Override
	public void sendToActionBar(Player p, String msg) throws Exception {
		//IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + msg + "\"}");
		Object icbc = chatMessageConstructor.newInstance(msg, new Object[0]);
		Object packet = packetPlayOutChatConstructor.newInstance(icbc, chatMessageTypeEnum, p.getUniqueId());

		// CraftPlayer p = (CraftPlayer) player;
		Object cp = playerClass.cast(p);
		// p.getHandle().playerConnection.sendPacket(ppoc);
		sendPacketMethod.invoke(playerConnectionField.get(
				getHandleMethod.invoke(cp)), packet);
	}
}
