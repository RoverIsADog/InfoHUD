package com.roverisadog.infohud.command;

import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum TimeMode {

    DISABLED(0, "disabled", "disabled"),
    CURRENT_TICK(1, "currentTick", "current tick"),
    CLOCK24(2, "clock24", "24 hour clock"),
    VILLAGER_SCHEDULE(3, "villagerSchedule", "villager schedule"),
    CLOCK12(4, "clock12", "12 hour clock");

    public static final List<String> OPTIONS_LIST = Arrays.stream(TimeMode.values())
            .map(TimeMode::toString)
            .collect(Collectors.toList());
    public static String cmdName = "time";
    public static String cfgKey = "timeMode";

    public final int id;
    public final String name;
    public final String description;

    TimeMode(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public static TimeMode get(int id) {
        for (TimeMode tm : TimeMode.values()) {
            if (tm.id == id) {
                return tm;
            }
        }
        return null;
    }

    public static TimeMode get(String id) {
        for (TimeMode tm : TimeMode.values()) {
            if (tm.name.equalsIgnoreCase(id)) {
                return tm;
            }
        }
        return null;
    }

    /**
     * @param player Player to check.
     * @return Local time of the player, in ticks.
     */
    public static String getTimeTicks(Player player) {
        return String.valueOf(player.getWorld().getTime());
    }

    /**
     * @param player Player to check.
     * @return Local time of the player, in HH:mm
     * @see <a href="https://minecraft.gamepedia.com/Daylight_cycle">Daylight Cycle</a>
     */
    public static String getTime24(Player player) {
        long time = player.getWorld().getTime();
        //MC day starts at 6:00: https://minecraft.gamepedia.com/Daylight_cycle
        String timeH = Long.toString((time / 1000L + 6L) % 24L);
        String timeM = String.format("%02d", time % 1000L * 60L / 1000L);
        return timeH + ":" + timeM;
    }

    /**
     * @param player Player to check.
     * @return Local time of the player, in hh:mm HH
     * @see <a href="https://minecraft.gamepedia.com/Daylight_cycle">Daylight Cycle</a>
     */
    public static String getTime12(Player player, String col1, String col2) {
        long time = player.getWorld().getTime();
        //MC day starts at 6:00
        boolean isPM = false;
        long currentHour = (time / 1000L + 6L) % 24L;
        if (currentHour > 12) {
            currentHour -= 12L;
            isPM = true;
        }
        String timeH = Long.toString(currentHour);
        String timeM = String.format("%02d", time % 1000L * 60L / 1000L);
        return col2 + timeH + ":" + timeM + col1 + (isPM ? " PM" : " AM");
    }

    /**
     * Returns the current villager behavior pattern and the time remaining until
     * the next scheduled behavior, using local player time.
     * @param player Player to check.
     * @param col1 Main display color.
     * @param col2 Secondary display color.
     * @see <a href="https://minecraft.gamepedia.com/Villager#Schedules">Villager schedule</a>
     */
    public static String getVillagerTime(Player player, String col1, String col2) {
        long time = player.getWorld().getTime();
        //Sleeping 12000 - 0
        if (time > 12000L) {
            long remaining = 12000L - time + 12000L;
            return col1 + "Sleep: " + col2 + remaining / 1200L + ":" + String.format("%02d", remaining % 1200L / 20L);
        }
        //Wandering 11000 - 12000
        else if (time > 11000L) {
            long remaining = 1000L - time + 11000L;
            return col1 + "Wander: " + col2 + 0 + ":" + String.format("%02d", remaining % 1200L / 20L);
        }
        //Gathering 9000 - 11000
        else if (time > 9000L) {
            long remaining = 2000L - time + 9000L;
            return col1 + "Gather: " + col2 + remaining / 1200L + ":" + String.format("%02d", remaining % 1200L / 20L);
        }
        //Working 2000 - 9000
        else if (time > 2000L) {
            long remaining = 7000L - time + 2000L;
            return col1 + "Work: " + col2 + remaining / 1200L + ":" + String.format("%02d", remaining % 1200L / 20L);
        }
        //Wandering 0 - 2000
        else {
            long remaining = 2000L - time;
            return col1 + "Wander: " + col2 + remaining / 1200L + ":" + String.format("%02d", remaining % 1200L / 20L);
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
