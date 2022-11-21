
package com.roverisadog.infohud;

import com.roverisadog.infohud.config.*;
import com.roverisadog.infohud.message.*;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class InfoHUD extends JavaPlugin {

	private static InfoHUD instance;

	public InfoHUD() {
		instance = this;
	}

	public static InfoHUD getPlugin() {
		return instance;
	}

	private boolean isSpigot;

	//Version for reflection
	private String versionStr;
	private ActionBarSender actionBarSender;

	// Tasks
	protected BukkitTask msgSenderTask;
	protected BukkitTask biomeUpdateTask;

	private PlayerConfig playerConfig;

	public PlayerConfig getPlayerConfig() {
		return playerConfig;
	}

	/**
	 * Initial setup:
	 * Load config, get version, get NMS packet classes.
	 * Configure CommandHandler
	 */
	@Override
	public void onEnable() {
		try {
			Util.printToTerminal(Util.GRN + "InfoHUD Enabling...");
			this.saveDefaultConfig(); // Does nothing if config.yml already exists.

			//Save initial cfg or load.
			this.playerConfig = new PlayerConfig(this);
			if (!playerConfig.loadConfig()) {
				throw new Exception(Util.ERR + "Error while reading config.yml.");
			}

			//Version check eg: org.bukkit.craftbukkit.v1_16_R2.blabla
			String ver = Bukkit.getServer().getClass().getPackage().getName();
			versionStr = ver.split("\\.")[3]; //v1_16_R2
			Util.apiVersion = Integer.parseInt(versionStr.split("_")[1]); //16
			Util.serverVendor = ver.split("\\.")[2]; //craftbukkit/spigot/paper

//			Util.printToTerminal("versionStr: %s, apiVersion: %s, serverVendor: %s"
//					, versionStr, Util.apiVersion, Util.serverVendor);

			// Either use spigot API or try to use NMS otherwise.
			if (!initializeActionBarSender()) {
				throw new Exception(Util.ERR + "Version error.");
			}

			// Setup command executor
			this.getCommand(Util.CMD_NAME).setExecutor(new CommandExecutor(this));

			//Start sender and biome updater tasks
			msgSenderTask = startMessageUpdaterTask(Util.getMessageUpdateDelay());
			biomeUpdateTask = getBiomeUpdaterTask(Util.getBiomeUpdateDelay());

			// Register action bar listener
			ActionBarListener abl = new ActionBarListener(actionBarSender);
			Bukkit.getServer().getPluginManager().registerEvents(abl, this);


			Util.printToTerminal(Util.GRN + "InfoHUD Successfully Enabled on "
					+ Util.WHI + (isSpigot ? "Spigot API" : "NMS")
					+ " v1." + Util.apiVersion);
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
	 * {@link PlayerCfg}. Cannot be asynchronous as it accesses
	 * non thread-safe methods from bukkit API.
	 * @param messageUpdateDelay config.yml: messageUpdateDelay
	 * @return BukkitTask created.
	 */
	public BukkitTask startMessageUpdaterTask(long messageUpdateDelay) {
		return Bukkit.getScheduler().runTaskTimer(instance, () -> {
			long benchmarkStart = System.nanoTime();
			for (Player p : instance.getServer().getOnlinePlayers()) {

				//Skip players that are not on the list
				if (!playerConfig.isEnabled(p)) {
					continue;
				}

				//Assumes that online players << saved players

				PlayerCfg cfg = playerConfig.getConfig(p);

				if (cfg.getCoordMode() == CoordMode.DISABLED && cfg.getTimeMode() == TimeMode.DISABLED) {
					playerConfig.removePlayer(p);
					continue;
				}

				//Setting dark mode colors -> Assume disabled : 0
				String color1; //Text
				String color2; //Values

				if (cfg.getDarkMode() == DarkMode.AUTO) {
					if (cfg.isInBrightBiome()) {
						color1 = Util.dark1;
						color2 = Util.dark2;
					}
					else {
						color1 = Util.bright1;
						color2 = Util.bright2;
					}
				}
				else if (cfg.getDarkMode() == DarkMode.DISABLED) {
					color1 = Util.bright1;
					color2 = Util.bright2;
				}
				else { //DarkMode.ENABLED
					color1 = Util.dark1;
					color2 = Util.dark2;
				}

				//Coordinates enabled
				if (cfg.getCoordMode() == CoordMode.ENABLED) {
					switch (cfg.getTimeMode()) {
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
				else if (cfg.getCoordMode() == CoordMode.DISABLED) {
					switch (cfg.getTimeMode()) {
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
	 * Gets the synchronous task responsible for fetching biomes. Relatively very expensive.
	 * Not asynchronous as it accesses non thread-safe methods from the bukkit API.
	 * @param biomeUpdateDelay Ticks between each player biomes updates, preferably larger.
	 *                         config.yml: biomeUpdateDelay
	 */
	private BukkitTask getBiomeUpdaterTask(long biomeUpdateDelay) {
		return Bukkit.getScheduler().runTaskTimer(instance, () -> {

			//getOnlinePlayers() not thread safe
			for (Player p : getServer().getOnlinePlayers()) {
				if (playerConfig.isEnabled(p)) {
					if (playerConfig.getConfig(p).getDarkMode() == DarkMode.AUTO) {
						playerConfig.updateIsInBrightBiome(p);
					}
				}
			}

		}, 0L, biomeUpdateDelay);
	}

	/**
	 * Utility method to initialise the ActionBarSender, depending on server
	 * version and vendor, can be spigot API wrapper (spigot/paper, preferred)
	 * or NMS (craftbukkit). NMS sender gotten through reflection and needs to
	 * be updated every update.
	 * @return Whether an {@link ActionBarSender} initialized correctly.
	 */
	private boolean initializeActionBarSender() {

		// Use spigot API when possible by checking Player$Spigot and method exists.
		// (Actionbar DNE MC < 1.9, and API DNE craftbukkit / early 1.9 spigot builds)
		try {
//			Player.Spigot.class.getMethod("sendMessage", ChatMessageType.class, BaseComponent.class);
			Class.forName("org.bukkit.entity.Player$Spigot").getDeclaredMethod(
					"sendMessage", ChatMessageType.class, BaseComponent.class); // Exists
//			Util.printToTerminal("Classname: " + Player.Spigot.class);
			isSpigot = true;
		} catch (Exception | Error ignored) {
//			ignored.printStackTrace();
			isSpigot = false;
		}

		// Prefer using spigot api when possible
		if (isSpigot) {
			Util.printToTerminal(Util.GRN + "Using Spigot API");
			actionBarSender = new ActionBarSenderSpigot();
		}
		// Fallback to NMS if using craftbukkit / very old spigot
		else {
			Util.printToTerminal(Util.GRN + "Spigot API unavailable or incompatible:" +
					"falling back to NMS");
			try {
				// 1.8 - 1.11
				if (Util.apiVersion < 12)
					actionBarSender = new ActionBarSenderNMS1_8(versionStr);
				// 1.11 - 1.15
				else if (Util.apiVersion < 16)
					actionBarSender = new ActionBarSenderNMS1_12(versionStr);
				// 1.16
				else if (Util.apiVersion < 17)
					actionBarSender = new ActionBarSenderNMS1_16(versionStr);
				// 1.17
				else if (Util.apiVersion < 18)
					actionBarSender = new ActionBarSenderNMS1_17(versionStr);
				else // FIXME update NMS
					actionBarSender = new ActionBarSenderNMS1_17(versionStr);

			} catch (Exception | Error e) { // Reflection error
				Util.printToTerminal(Util.ERR + "Exception while initializing packets with" +
						"NMS v1." + versionStr + ". Version may be incompatible.");
				e.printStackTrace();
				return false;
			}
		}

		return true;
	}

	/**
	 * Stops all tasks, reloads content of config.yml and restarts tasks with the updated values.
	 * @param sender Sender who issued the reload command.
	 */
	public void reload(CommandSender sender) {
		boolean success;
		try {
			// Cancel tasks, reload config, and restart tasks.
			msgSenderTask.cancel();
			biomeUpdateTask.cancel();
			success = playerConfig.loadConfig();
			msgSenderTask = startMessageUpdaterTask(Util.getMessageUpdateDelay());
			biomeUpdateTask = getBiomeUpdaterTask(Util.getBiomeUpdateDelay());

			if (success) {
				Util.sendMsg(sender, Util.GRN + "Reloaded successfully.");
			}
			else {
				Util.sendMsg(sender, Util.ERR + "Reload failed.");
			}
		}
		catch (Exception e) {
			Util.sendMsg(sender, Util.ERR + "An internal error has occurred.");
			e.printStackTrace();
		}
	}

	/**
	 * Sends a message to a player's actionbar using {@link ActionBarSender}
	 * @param p Recipient player.
	 * @param msg Message to send (as is).
	 */
	private void sendToActionBar(Player p, String msg) {
		try {
			actionBarSender.sendToActionBar(p, msg);
		} catch (Exception e) {
			Util.printToTerminal("Fatal error while sending packets. Shutting down...");
			Bukkit.getPluginManager().disablePlugin(instance);
			e.printStackTrace();
		}
	}

}
