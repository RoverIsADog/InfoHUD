package com.roverisadog.infohud.command;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum CoordMode {

    DISABLED(0, "disabled"),
    ENABLED(1, "enabled");

    public static List<String> stringList = Arrays.stream(CoordMode.values())
            .map(CoordMode::getName)
            .collect(Collectors.toList());
    public static String cmdName = "coordinates";
    public static String cfgName = "coordinatesMode";

    public String name;
    public int id;

    CoordMode(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public static CoordMode get(int id) {
        for (CoordMode cm : CoordMode.values()) {
            return cm;
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}
