package com.roverisadog.infohud.command;

import com.roverisadog.infohud.Util;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum TimeMode {

    DISABLED(0,
            "disabled",
            (p, col1, col2) -> ""),
    CURRENT_TICK(1,
            "currentTick",
            (p, col1, col2) -> String.valueOf(p.getWorld().getTime())),
    CLOCK24(2,
            "clock24",
            (p, col1, col2) -> Util.getTime24(p.getWorld().getTime())),
    VILLAGER_SCHEDULE(3,
            "clock24",
            (p, col1, col2) -> Util.getVillagerTimeLeft(p.getWorld().getTime(), col1, col2)),
    CLOCK12(4,
            "clock12",
            (p, col1, col2) -> Util.getTime12(p.getWorld().getTime()));

    public static List<String> stringList = Arrays.stream(TimeMode.values())
            .map(TimeMode::name)
            .collect(Collectors.toList());
    public static String cmdName = "time";
    public static String cfgKey = "timeMode";

    public final String name;
    public final int id;
    public final StringCreator stringCreator;

    TimeMode(int id, String name, StringCreator stringCreator) {
        this.id = id;
        this.name = name;
        this.stringCreator = stringCreator;
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

    @Override
    public String toString() {
        return name;
    }

    public String toString(Player p, String col1, String col2) {
        return stringCreator.getString(p, col1, col2);
    }
}
