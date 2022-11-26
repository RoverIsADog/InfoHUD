package com.roverisadog.infohud.message;

import com.roverisadog.infohud.InfoHUD;
import com.roverisadog.infohud.Util;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;

/**
 * <b>The events in this file are present in craftbukkit .</b>
 * Contains event listeners that will pause actionbar message delivery to one or all players in
 * reaction to some events (entering bed, mounting mob, etc.) to not overwrite minecraft's own
 * action bar messages for these events.
 * For the above to work, the assumption that the bukkit event manager is run before the server
 * sends its own action bar messages must hold.
 */
public class PauseListener implements Listener {

	/**
	 * Whether the current version of minecraft sends a message to the actionbar when failing to
	 * enter a bed so that InfoHUD gets paused to the player who failed to enter the bed.
	 * 'You can only sleep at night', 'Bed is too far', 'Monsters nearby' and 'Bed occupied' were
	 * added in 1.11.
	 */
	public static boolean hasEnterBedFailMessage = false;

	/**
	 * Whether the current version of minecraft sends a message to the actionbar of all players
	 * when a player starts sleeping so that InfoHUD gets paused for all players upon a player
	 * successfully entering a bed. 'x/y players sleeping' and 'Sleeping through the night' were
	 * added in 1.17.
	 */
	public static boolean hasGlobalSleepMessage = false;
	private final InfoHUD pluginInstance;

	public PauseListener(InfoHUD pluginInstance) {
		this.pluginInstance = pluginInstance;
		if (InfoHUD.apiVersion > 10) hasEnterBedFailMessage = true;
		if (InfoHUD.apiVersion > 16) hasGlobalSleepMessage = true;
//		Util.printToTerminal("PauseListener initialized");
	}

	@EventHandler
	public void onBedEnterEvent(PlayerBedEnterEvent e) {
//		Util.printToTerminal("onBedEnterEvent triggered");
		// Successfully entered bed -> Pause for everyone (1.17+)
		if (!e.isCancelled()) {
			if (hasGlobalSleepMessage) {
//				Util.printToTerminal("Pausing for all players");
				pluginInstance.getConfigManager().pauseFor(60);
			}
		}
		// Failed to enter bed -> Pause for initiating player (1.11+).
		else {
			if (hasEnterBedFailMessage) {
//				Util.printToTerminal("Pausing for " + e.getPlayer().getName());
				pluginInstance.getConfigManager().pauseFor(e.getPlayer(), 60);
			}
		}
	}

}
