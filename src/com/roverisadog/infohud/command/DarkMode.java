package com.roverisadog.infohud.command;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum DarkMode {

    DISABLED(0, "disabled"),
    ENABLED(1, "enabled"),
    AUTO(2, "auto");

    public static List<String> stringList = Arrays.stream(DarkMode.values())
            .map(DarkMode::getName)
            .collect(Collectors.toList());
    public static String cmdName = "darkMode";
    public static String cfgName = "darkMode";

    public int id;
    public String name;

    DarkMode(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static DarkMode get(int id) {
        for (DarkMode dm : DarkMode.values()) {
            return dm;
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}
