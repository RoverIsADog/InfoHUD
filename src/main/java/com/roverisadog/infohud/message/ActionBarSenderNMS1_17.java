package com.roverisadog.infohud.message;

import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * NMS for sending messages to the actionbar, for 1.17
 */
public class ActionBarSenderNMS1_17 implements ActionBarSender {

	private final Class<?> craftPlayerClass;
	private final Method getHandleMethod;
	private final Field playerConnectionField;
	private final Method sendPacketMethod;
	private final Constructor<?> packetPlayOutChatConstructor;
	private final Constructor<?> chatMessageConstructor;
	private final Object charMessageTypeEnum;

	/**
	 * Initialize NMS if applicable.
	 * @param versionStr Internal string for the server version.
	 * @throws Exception Reflection errors or otherwise.
	 */
	public ActionBarSenderNMS1_17(String versionStr) throws Exception {
		String nmsPath = "net.minecraft."; // No longer in server package.

		//org.bukkit.craftbukkit.VERSION.entity.CraftPlayer; | CraftPlayer p = (CraftPlayer) player;
		craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + versionStr + ".entity.CraftPlayer");

		//import net.minecraft.server.v1_16_R2.IChatBaseComponent;
		Class<?> iChatBaseComponentClass = Class.forName(nmsPath + "network.chat.IChatBaseComponent");

		//import net.minecraft.server.v1_16_R2.Packet
		Class<?> packetClass = Class.forName(nmsPath + "network.protocol.Packet");

		// p.getHandle().playerConnection.sendPacket(ppoc);
		getHandleMethod = craftPlayerClass.getMethod("getHandle");
		playerConnectionField = getHandleMethod.getReturnType().getField("b"); // playerConnection obfuscated to b
		sendPacketMethod = playerConnectionField.getType().getMethod("sendPacket", packetClass);

		// IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + msg + "\"}");
		Class<?> chatMessageClass = Class.forName(nmsPath + "network.chat.ChatMessage");
		chatMessageConstructor = chatMessageClass.getConstructor(String.class, Object[].class);

		// import net.minecraft.server.VERSION.IChatBaseComponent;
		Class<?> packetPlayOutChatClass = Class.forName(nmsPath+ "network.protocol.game.PacketPlayOutChat");

		// import net.minecraft.server.v1_16_R2.ChatMessageType;
		Class<?> chatMessageTypeClass = Class.forName(nmsPath + "network.chat.ChatMessageType");
		// ChatMessageType.GAME_INFO: 2nd index of enum
		charMessageTypeEnum = chatMessageTypeClass.getEnumConstants()[2];
		// PacketPlayOutChat ppoc = new PacketPlayOutChat(icbc, ChatMessageType.GAME_INFO);
		packetPlayOutChatConstructor = packetPlayOutChatClass
				.getConstructor(iChatBaseComponentClass, chatMessageTypeClass, UUID.class);

	}

	@Override
	public void sendToActionBar(Player p, String msg) throws Exception {
		//IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + msg + "\"}");
		Object icbc = chatMessageConstructor.newInstance(msg, new Object[0]);
		Object packet = packetPlayOutChatConstructor.newInstance(icbc, charMessageTypeEnum, p.getUniqueId());

		// CraftPlayer p = (CraftPlayer) player;
		Object cp = craftPlayerClass.cast(p);
		// p.getHandle().playerConnection.sendPacket(ppoc);
		sendPacketMethod.invoke(playerConnectionField.get(
				getHandleMethod.invoke(cp)), packet);
	}
}
