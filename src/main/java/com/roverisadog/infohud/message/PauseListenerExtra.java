package com.roverisadog.infohud.message;

import com.roverisadog.infohud.InfoHUD;
import com.roverisadog.infohud.Util;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.spigotmc.event.entity.EntityMountEvent;

/**
 * <b>The events in this file are not present in craftbukkit but present in spigot.</b>
 * Contains event listeners that will pause actionbar message delivery to one or all players in
 * reaction to some events (entering bed, mounting mob, etc.) to not overwrite minecraft's own
 * action bar messages for these events.
 * For the above to work, the assumption that the bukkit event manager is run before the server
 * sends its own action bar messages must hold.
 */
public class PauseListenerExtra implements Listener {
	private final InfoHUD pluginInstance;

	public PauseListenerExtra(InfoHUD pluginInstance) {
		this.pluginInstance = pluginInstance;
//		Util.printToTerminal("PauseListenerExtra registered");
	}

	/** Event doesn't exist on craftbukkit, will simply fail to register listener.  */
	@EventHandler
	public void onMountEnterEvent(EntityMountEvent e) {
		// Pause if a player enters a horse or pig.
		if (e.getEntityType() == EntityType.PLAYER && !e.isCancelled()) {
//			Util.printToTerminal("Pausing for " + e.getEntity().getName());
			pluginInstance.getConfigManager().pauseFor((Player) e.getEntity(), 60);
		}
	}

}
