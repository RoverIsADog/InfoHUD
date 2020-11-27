package com.roverisadog.infohud.command;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum TimeMode {

    DISABLE(0, "disabled"),
    CURRENT_TICK(1, "currentTick"),
    CLOCK24(2, "clock24"),
    VILLAGER_SCHEDULE(3, "clock24"),
    CLOCK12(4, "clock12");

    public static List<String> stringList = Arrays.stream(TimeMode.values())
            .map(TimeMode::getName)
            .collect(Collectors.toList());
    public static String cmdName = "time";
    public static String cfgName = "timeMode";

    public String name;
    public int id;

    TimeMode(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public static TimeMode get(int id) {
        for (TimeMode tm : TimeMode.values()) {
            return tm;
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}
