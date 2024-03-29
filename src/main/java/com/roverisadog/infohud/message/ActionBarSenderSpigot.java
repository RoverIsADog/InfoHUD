package com.roverisadog.infohud.message;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

/**
 * Sends a message using spigot api (preferred)
 */
public class ActionBarSenderSpigot implements ActionBarSender {

	@Override
	public void sendToActionBar(Player p, String msg) {
		p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
	}
}
