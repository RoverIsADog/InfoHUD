package com.roverisadog.infohud.message;

import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * NMS for sending messages to the actionbar, for 1.17+
 */
public class ActionBarSenderNMS1_19 implements ActionBarSender {

	private final Class<?> playerClass;
	private final Method getHandleMethod;
	private final Field playerConnectionField;
	private final Method sendPacketMethod;

	private final Method iChatBaseComponentGetter;
	private final Constructor<?> clientboundSetActionBarTextPacketConstructor;

	/**
	 * Initialize NMS if applicable.
	 * @param versionStr Internal string for the server version.
	 * @throws Exception Reflection errors or otherwise.
	 */
	public ActionBarSenderNMS1_19(String versionStr) throws Exception {
		String nmsPath = "net.minecraft.network.";

		/* --------------------------------- Player Related --------------------------------- */

		//org.bukkit.craftbukkit.entity.Player; | Player p = player;
		playerClass = Class.forName("org.bukkit.craftbukkit." + versionStr + ".entity.CraftPlayer");

		//import net.minecraft.server.v1_16_R2.Packet
		Class<?> packetClass = Class.forName(nmsPath + "protocol.Packet");

		// p.getHandle().playerConnection.sendPacket(ppoc);
		getHandleMethod = playerClass.getMethod("getHandle");
		playerConnectionField = getHandleMethod.getReturnType().getField("b");
		sendPacketMethod = playerConnectionField.getType().getMethod("a", packetClass);

		/* --------------------------------- Packet Related --------------------------------- */

		//import net.minecraft.server.v1_16_R2.IChatBaseComponent;
		Class<?> iChatBaseComponentClass = Class.forName(nmsPath + "chat.IChatBaseComponent"); //

		// IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + msg + "\"}");
		iChatBaseComponentGetter = iChatBaseComponentClass.getDeclaredClasses()[0].getMethod("a", String.class);


		Class<?> clientboundSetActionBarTextPacketClass = Class.forName(nmsPath + "protocol.game.ClientboundSetActionBarTextPacket");
		clientboundSetActionBarTextPacketConstructor = clientboundSetActionBarTextPacketClass.getConstructor(iChatBaseComponentClass);

	}

	@Override
	public void sendToActionBar(Player p, String msg) throws Exception {
		//IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + msg + "\"}");
		Object icbc = iChatBaseComponentGetter.invoke(null, "{\"text\": \"" + msg + "\"}");
		Object packet = clientboundSetActionBarTextPacketConstructor.newInstance(icbc);

		// CraftPlayer p = (CraftPlayer) player;
		Object cp = playerClass.cast(p);
		// p.getHandle().playerConnection.sendPacket(ppoc);
		sendPacketMethod.invoke(playerConnectionField.get(
				getHandleMethod.invoke(cp)), packet);
	}
}
