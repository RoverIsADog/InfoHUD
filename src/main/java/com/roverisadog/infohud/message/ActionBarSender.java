package com.roverisadog.infohud.message;

import org.bukkit.entity.Player;

public interface ActionBarSender {

	/**
	 * Sends a message to the action bar.
	 * @param p Target player.
	 * @param msg Message to send.
	 * @throws Exception Any exception while sending.
	 */
	void sendToActionBar(Player p, String msg) throws Exception;

}
