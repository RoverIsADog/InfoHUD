
package com.roverisadog.infohud;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class InfoHUD extends JavaPlugin {

    @Override
    public void onEnable() {
        this.getLogger().info("InfoHUD Enabled");
        this.saveDefaultConfig();
        Util.readConfig(this.getConfig(), this);
        this.getCommand(Util.CMD_NAME).setExecutor(new CommandHandler());
        this.run(this);
    }

    @Override
    public void onDisable() {
        this.getLogger().info("InfoHUD Disabled");
    }

    public void run(final Plugin plugin) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {

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

                    //Setting colors -> Assume disabled : 0
                    String col1 = Util.GLD; //Text
                    String col2 = Util.WHI; //Numbers

                    if (darkMode == 1){ //enabled : 1
                        col1 = Util.DAQA;
                        col2 = Util.AQA;
                    }
                    else if (darkMode == 2){ //auto : 2
                        boolean bright = Util.brightBiomes.containsKey(p.getLocation().getBlock().getBiome().toString());
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
                        if (timeMode == 1){
                            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, (new ComponentBuilder( col2 + Long.toString(p.getWorld().getTime()) ).create()));
                        }
                        else if (timeMode == 2){
                            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, (new ComponentBuilder( col2 + Util.getTime24(p.getWorld().getTime()) ).create()));
                        }
                        else if (timeMode == 3){
                            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, (new ComponentBuilder( col2 + Util.getVillagerTimeLeft(p.getWorld().getTime(), col1, col2) ).create()));
                        }

                    }
                }
            }

        }, 0L, Util.getRefreshRate());
    }

}
