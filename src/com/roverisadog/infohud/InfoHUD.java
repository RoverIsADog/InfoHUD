
package com.roverisadog.infohud;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

public class InfoHUD extends JavaPlugin {

    @Override
    public void onEnable() {
        this.saveDefaultConfig(); //Silent fail if config.yml already exists
        this.getLogger().info(Util.SIGNATURE + "InfoHUD Enabling...");
        if (!Util.loadConfig(this)) this.getLogger().info(Util.ERROR + "Error while reading config.yml...");
        Objects.requireNonNull(this.getCommand(Util.CMD_NAME)).setExecutor(new CommandHandler());
        Util.task = start(this);
        this.getLogger().info(Util.SIGNATURE + "InfoHUD Enabled");
    }

    @Override
    public void onDisable() {
        this.getLogger().info(Util.SIGNATURE + "InfoHUD Disabled" + Util.RES);
    }

    public BukkitTask start(final Plugin plugin) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {

            public void run(){
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    //Skip players that are not on the list
                    if (!Util.isOnList(p)) continue;
                    /* Assumes that online players << saved players */

                    int[] playerCfg = Util.getCFG(p);
                    int coordsMode = playerCfg[0];
                    int timeMode = playerCfg[1];
                    int darkMode = playerCfg[2];

                    if (coordsMode == 0 && timeMode == 0) {
                        Util.removePlayer(p);
                        continue;
                    }

                    //Setting dark mode colors -> Assume disabled : 0
                    String col1 = Util.GLD; //Text
                    String col2 = Util.WHI; //Values

                    if (darkMode == 1){ //enabled : 1
                        col1 = Util.DAQA;
                        col2 = Util.AQA;
                    }
                    else if (darkMode == 2){ //auto : 2
                        boolean bright = Util.brightBiomes.containsKey(p.getLocation().getBlock().getBiome());
                        col1 = bright ? Util.DBLU : Util.GLD;
                        col2 = bright ? Util.DAQA : Util.WHI;
                    }

                    //Coordinates enabled
                    if (coordsMode == 1){
                        //Only display coords
                        if (timeMode == 0){
                            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, (new ComponentBuilder(
                                    col1 + "XYZ: " + col2 + Util.getCoordinates(p) ).create()));
                        }
                        //Display coords and time in ticks
                        else if (timeMode == 1){
                            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, (new ComponentBuilder(
                                    col1 + "XYZ: " + col2 + Util.getCoordinates(p) + " " +
                                            col1 + String.format("%-10s", Util.getPlayerDirection(p)) + col2 + p.getWorld().getTime()) ).create());
                        }
                        //Display coords and time in HH:mm
                        else if (timeMode == 2){
                            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, (new ComponentBuilder(
                                    col1 + "XYZ: " + col2 + Util.getCoordinates(p) + " " +
                                            col1 + String.format("%-10s", Util.getPlayerDirection(p)) + col2 + Util.getTime24(p.getWorld().getTime()) ).create()));
                        }
                        //Display coords and villager timer
                        else if (timeMode == 3){
                            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, (new ComponentBuilder(
                                    col1 + "XYZ: " + col2 + Util.getCoordinates(p) + " " +
                                            col1 + String.format("%-10s", Util.getPlayerDirection(p)) + Util.getVillagerTimeLeft(p.getWorld().getTime(), col1, col2) ).create()));
                        }
                    }

                    //Coordinates disabled
                    else if (coordsMode == 0){
                        //Display time in ticks
                        if (timeMode == 1){
                            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, (new ComponentBuilder( col2 + p.getWorld().getTime()).create()));
                        }
                        //Display time in HH:mm
                        else if (timeMode == 2){
                            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, (new ComponentBuilder( col2 + Util.getTime24(p.getWorld().getTime()) ).create()));
                        }
                        //Display villager timer
                        else if (timeMode == 3){
                            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, (new ComponentBuilder( col2 + Util.getVillagerTimeLeft(p.getWorld().getTime(), col1, col2) ).create()));
                        }
                    }
                }
            }
        }, 0L, Util.getRefreshRate());
    }

}
