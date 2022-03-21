
package com.roverisadog.infohud;

import com.roverisadog.infohud.command.CoordMode;
import com.roverisadog.infohud.command.DarkMode;
import com.roverisadog.infohud.command.TimeMode;
import com.roverisadog.infohud.message.ActionBarSender;
import com.roverisadog.infohud.message.ActionBarSenderNMS1_12;
import com.roverisadog.infohud.message.ActionBarSenderNMS1_16;
import com.roverisadog.infohud.message.ActionBarSenderNMS1_17;
import com.roverisadog.infohud.message.ActionBarSenderNMS1_8;
import com.roverisadog.infohud.message.ActionBarSenderSpigot;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;

public class InfoHUD extends JavaPlugin {

	protected static InfoHUD instance;

	public InfoHUD() {
		instance = this;
	}

	private static boolean isSpigot;

	//Version for reflection
	private static String versionStr;
	private static ActionBarSender actionBar;

	/** Time elapsed for the last update. */
	private long benchmarkStart;

	protected BukkitTask msgSenderTask;
	protected BukkitTask biomeUpdateTask;

	/**
	 * Initial setup:
	 * Load config, get version, get NMS packet classes.
	 * Configure CommandHandler
	 */
	@Override
	public void onEnable() {
		try {
			Util.printToTerminal(Util.GRN + "InfoHUD Enabling...");

			//Save initial cfg or load.
			this.saveDefaultConfig(); //Silent fails if config.yml already exists
			if (!Util.loadConfig()) {
				throw new Exception(Util.ERR + "Error while reading config.yml.");
			}

			//Version check
			//Eg: org.bukkit.craftbukkit.v1_16_R2.blabla
			String ver = Bukkit.getServer().getClass().getPackage().getName();
			versionStr = ver.split("\\.")[3]; //v1_16_R2
			Util.apiVersion = Integer.parseInt(versionStr.split("_")[1]); //16
			Util.serverVendor = ver.split("\\.")[2]; //craftbukkit/spigot/paper

			//Attempt to get version-specific NMS packets class.
			if (!initializeActionBarSender()) {
				throw new Exception(Util.ERR + "Version error.");
			}

			//Setup command executor
			this.getCommand(Util.CMD_NAME).setExecutor(new CommandExecutor(this));

			//Start sender and biome updater tasks
			msgSenderTask = startMessageUpdaterTask(Util.getMessageUpdateDelay());
			biomeUpdateTask = startBiomeUpdaterTask(Util.getBiomeUpdateDelay());


			Util.printToTerminal(Util.GRN + "InfoHUD Successfully Enabled on "
					+ Util.WHI + (isSpigot ? "Spigot API" : "NMS")
					+ " Version 1." + Util.apiVersion);
		}
		catch (Exception e) {
			Util.printToTerminal(e.getMessage());
			Util.printToTerminal("Shutting down...");
			Bukkit.getPluginManager().disablePlugin(this);
		}
	}

	/** Clean up while shutting down (Currently nothing). */
	@Override
	public void onDisable() {
		Util.printToTerminal(Util.GRN + "InfoHUD Disabled");
	}

	/**
	 * Starts a synchronous task whose job is to get each player's config and
	 * send the right message accordingly. Does NOT change any value from the
	 * {@link PlayerCfg}. Note to self: Don't make asynchronous as it accesses
	 * non thread-safe methods from bukkit API.
	 * @param messageUpdateDelay config.yml: messageUpdateDelay
	 * @return BukkitTask created.
	 */
	public BukkitTask startMessageUpdaterTask(long messageUpdateDelay) {
		return Bukkit.getScheduler().runTaskTimer(instance, () -> {
			benchmarkStart = System.nanoTime();
			for (Player p : instance.getServer().getOnlinePlayers()) {

				//Skip players that are not on the list
				if (!PlayerCfg.isEnabled(p)) {
					continue;
				}

				//Assumes that online players << saved players

				PlayerCfg cfg = PlayerCfg.getConfig(p);

				if (cfg.coordMode == CoordMode.DISABLED && cfg.timeMode == TimeMode.DISABLED) {
					PlayerCfg.removePlayer(p);
					continue;
				}

				//Setting dark mode colors -> Assume disabled : 0
				String color1; //Text
				String color2; //Values

				if (cfg.darkMode == DarkMode.AUTO) {
					if (cfg.isInBrightBiome) {
						color1 = Util.dark1;
						color2 = Util.dark2;
					}
					else {
						color1 = Util.bright1;
						color2 = Util.bright2;
					}
				}
				else if (cfg.darkMode == DarkMode.DISABLED) {
					color1 = Util.bright1;
					color2 = Util.bright2;
				}
				else { //DarkMode.ENABLED
					color1 = Util.dark1;
					color2 = Util.dark2;
				}

				//Coordinates enabled
				if (cfg.coordMode == CoordMode.ENABLED) {
					switch (cfg.timeMode) {
						case DISABLED:
							sendToActionBar(p, color1 + "XYZ: "
									+ color2 + CoordMode.getCoordinates(p) + " "
									+ color1 + Util.getPlayerDirection(p));
							break;
						case CURRENT_TICK:
							sendToActionBar(p, color1 + "XYZ: "
									+ color2 + CoordMode.getCoordinates(p) + " "
									+ color1 + String.format("%-10s", Util.getPlayerDirection(p))
									+ color2 + TimeMode.getTimeTicks(p));
							break;
						case CLOCK24:
							sendToActionBar(p, color1 + "XYZ: "
									+ color2 + CoordMode.getCoordinates(p) + " "
									+ color1 + String.format("%-10s", Util.getPlayerDirection(p))
									+ color2 + TimeMode.getTime24(p));
							break;
						case CLOCK12:
							sendToActionBar(p, color1 + "XYZ: "
									+ color2 + CoordMode.getCoordinates(p) + " "
									+ color1 + String.format("%-10s", Util.getPlayerDirection(p))
									+ color2 + TimeMode.getTime12(p, color1, color2));
							break;
						case VILLAGER_SCHEDULE:
							sendToActionBar(p, color1 + "XYZ: "
									+ color2 + CoordMode.getCoordinates(p) + " "
									+ color1 + String.format("%-10s", Util.getPlayerDirection(p))
									+ color2 + TimeMode.getVillagerTime(p, color1, color2));
							break;
						default: //Ignored
					}
				}

				//Coordinates disabled
				else if (cfg.coordMode == CoordMode.DISABLED) {
					switch (cfg.timeMode) {
						case CURRENT_TICK:
							sendToActionBar(p, color2 + TimeMode.getTimeTicks(p));
							break;
						case CLOCK12:
							sendToActionBar(p, color2 + TimeMode.getTime12(p, color1, color2));
							break;
						case CLOCK24:
							sendToActionBar(p, color2 + TimeMode.getTime24(p));
							break;
						case VILLAGER_SCHEDULE:
							sendToActionBar(p, color2 + TimeMode.getVillagerTime(p, color1, color2));
							break;
						default: //Ignored
					}
				}
			}
			Util.benchmark = System.nanoTime() - benchmarkStart;
		}, 0L, messageUpdateDelay);
	}

	/**
	 * Starts the synchronous task responsible for fetching biomes. Very
	 * expensive. Note to self: Don't make asynchronous as it accesses non
	 * thread-safe methods from bukkit API.
	 * @param biomeUpdateDelay Ticks between each player biomes updates.
	 *                         Preferably larger. config.yml: biomeUpdateDelay
	 */
	public BukkitTask startBiomeUpdaterTask(long biomeUpdateDelay) {
		return Bukkit.getScheduler().runTaskTimer(instance, () -> {

			for (Player p : instance.getServer().getOnlinePlayers()) {
				if (PlayerCfg.isEnabled(p)) {
					if (PlayerCfg.getConfig(p).darkMode == DarkMode.AUTO) {
						Util.updateIsInBrightBiome(p);
					}
				}
			}

		}, 0L, biomeUpdateDelay);
	}

	/**
	 *  Uses reflection to get version specific NMS packet-related classes.
	 *  Preferred to Spigot built in method for wider compatibility.
	 */
	private static boolean initializeActionBarSender() {

		// Use spigot API when possible by checking method exists (DNE in 1.8, early spigot builds for 1.9)
		try {
			Player.Spigot.class.getDeclaredMethod("sendMessage", ChatMessageType.class, BaseComponent.class);
			isSpigot = true;
		} catch (Exception ignored) {
			isSpigot = false;
		}

		// Prefer using spigot api when possible
		if (isSpigot) {
			Util.printToTerminal(Util.GRN + "Using Spigot API");
			actionBar = new ActionBarSenderSpigot();
		}
		// Fallback to NMS if using craftbukkit / very old spigot
		else {
			Util.printToTerminal(Util.GRN + "Spigot API unavailable or incompatible: falling back to NMS");
			try {
				// 1.8 - 1.11
				if (Util.apiVersion < 12)
					actionBar = new ActionBarSenderNMS1_8(versionStr);
				// 1.11 - 1.15
				else if (Util.apiVersion < 16)
					actionBar = new ActionBarSenderNMS1_12(versionStr);
				// 1.16
				else if (Util.apiVersion < 17)
					actionBar = new ActionBarSenderNMS1_16(versionStr);
				// 1.17+ CHECK FOR CHANGES EACH UPDATES!!!
				else
					actionBar = new ActionBarSenderNMS1_17(versionStr);

			} catch (Exception e) { // Reflection error
				Util.printToTerminal(Util.ERR + "Exception while initializing packets with NMS version 1."
						+ versionStr + ". Version may be incompatible.");
				e.printStackTrace();
				return false;
			}
		}

		return true;
	}

	/**
	 * Sends a message to the player's actionbar using reflected methods.
	 * @param p Recipient player.
	 * @param msg Message to send.
	 */
	private static void sendToActionBar(Player p, String msg) {
		try {
			actionBar.sendToActionBar(p, msg);
		} catch (Exception e) {
			Util.printToTerminal("Fatal error while sending packets. Shutting down...");
			Bukkit.getPluginManager().disablePlugin(instance);
			e.printStackTrace();
		}
	}

}
