package com.roverisadog.infohud;

import java.util.*;

import com.roverisadog.infohud.config.*;
import com.roverisadog.infohud.message.ActionBarSender;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class MessageUpdaterTask implements Runnable {

	/** Key in the config for the actionbar message sender task frequency (long).  */
	public static final String MESSAGE_UPDATE_DELAY_PATH = "messageUpdateDelay";
	/** Default value in case the key-value is missing in the config. */
	public static final int DEFAULT_MESSAGE_UPDATE_DELAY = 5;
	/** Delay between each actionbar message update. */
	public static long messageUpdateDelay;
	//Colors
	public static String bright1 = Util.GLD;
	public static String bright2 = Util.WHI;
	public static String dark1 = Util.DBLU;
	public static String dark2 = Util.DAQA;


	/** Main instance of the InfoHUD plugin. */
	private final InfoHUD pluginInstance;
	/** Object containing the configurations of all players. */
	private final ConfigManager configManager;
	/** {@link #messageUpdateDelay} for this task instance. */
	private final int taskMessageUpdateDelay;
	/** How long the last message updater task iteration took. */
	public static long benchmark = 0;

	public MessageUpdaterTask(InfoHUD pluginInstance, int taskMessageUpdateDelay) {
		this.pluginInstance = pluginInstance;
		this.configManager = pluginInstance.getConfigManager();
		this.taskMessageUpdateDelay = taskMessageUpdateDelay;
	}

	/** @return How many ticks between each message update. */
	protected static long getMessageUpdateDelay() {
		return messageUpdateDelay;
	}

	@Override
	public void run() {

		long benchmarkStart = System.nanoTime();
		for (Player p : pluginInstance.getServer().getOnlinePlayers()) {

			//Skip players that are not on the list
			if (!configManager.updateAndGetEnabled(p, taskMessageUpdateDelay)) {
				continue;
			}

			//Assumes that online players << saved players

			PlayerCfg cfg = configManager.getCfg(p);

			if (cfg.getCoordMode() == CoordMode.DISABLED && cfg.getTimeMode() == TimeMode.DISABLED) {
				configManager.removePlayer(p);
				continue;
			}

			//Setting dark mode colors -> Assume disabled : 0
			String color1; //Text
			String color2; //Values

			if (cfg.getDarkMode() == DarkMode.AUTO) {
				if (cfg.isInBrightBiome()) {
					color1 = dark1;
					color2 = dark2;
				}
				else {
					color1 = bright1;
					color2 = bright2;
				}
			}
			else if (cfg.getDarkMode() == DarkMode.DISABLED) {
				color1 = bright1;
				color2 = bright2;
			}
			else { //DarkMode.ENABLED
				color1 = dark1;
				color2 = dark2;
			}

			List<String> fields = new ArrayList<>();
			if (cfg.getCoordMode() == CoordMode.ENABLED) {
				fields.add(color1 + "XYZ: " + color2 + CoordMode.getCoordinates(p));
				fields.add(color1 + String.format("%-10s", getPlayerDirection(p)));
			}
			switch (cfg.getTimeMode()) {
				case CURRENT_TICK:
					fields.add(color2 + TimeMode.getTimeTicks(p));
					break;
				case CLOCK24:
					fields.add(color2 + TimeMode.getTime24(p));
					break;
				case CLOCK12:
					fields.add(color2 + TimeMode.getTime12(p, color1, color2));
					break;
				case VILLAGER_SCHEDULE:
					fields.add(color2 + TimeMode.getVillagerTime(p, color1, color2));
					break;
			}

			String msg = String.join(" ", fields).trim();
			if (!msg.isEmpty()) {
				sendToActionBar(p, msg);
			}
		}
		benchmark = System.nanoTime() - benchmarkStart;
	}

	/**
	 * Calculates cardinal direction the player is facing.
	 * @return Small message indicating cardinal direction and coordinate
	 *         towards which the player is facing.
	 */
	private static String getPlayerDirection(Player player) {
		//-180: Leaning left | +180: Leaning right
		float yaw = player.getLocation().getYaw();
		//Bring to 360 degrees (Clockwise from -X axis)
		if (yaw < 0.0F) {
			yaw += 360.0F;
		}
		//Separate into 8 sectors (Arc: 45deg), offset by 1/2 sector (Arc: 22.5deg)
		int sector = (int) ((yaw + 22.5F) / 45F);
		switch (sector) {
			case 1: return "SW";
			case 2: return "W [-X]";
			case 3: return "NW";
			case 4: return "N [-Z]";
			case 5: return "NE";
			case 6: return "E [+X]";
			case 7: return "SE";
			case 0:
			default: //Example: (359 + 22.5)/45
				return "S [+Z]";
		}
	}


	/**
	 * Sends a message to a player's actionbar using {@link ActionBarSender}
	 * @param p Recipient player.
	 * @param msg Message to send (as is).
	 */
	private void sendToActionBar(Player p, String msg) {
		try {
			InfoHUD.actionBarSender.sendToActionBar(p, msg);
		} catch (Exception e) {
			Util.printToTerminal("Fatal error while sending packets. Shutting down...");
			Bukkit.getPluginManager().disablePlugin(pluginInstance);
			e.printStackTrace();
		}
	}
}
