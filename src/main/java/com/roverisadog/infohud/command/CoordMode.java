package com.roverisadog.infohud.command;

import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum CoordMode {

    DISABLED(0, "disabled", "disabled"),
    ENABLED(1, "enabled", "enabled");


    public static final List<String> OPTIONS_LIST = Arrays.stream(CoordMode.values())
            .map(CoordMode::toString)
            .collect(Collectors.toList());
    public static String cmdName = "coordinates";
    public static String cfgKey = "coordinatesMode";

    public int id;
    public String name;
    public String description;

    CoordMode(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public static CoordMode get(int id) {
        for (CoordMode cm : CoordMode.values()) {
            if (cm.id == id) {
                return cm;
            }
        }
        return null;
    }

    public static CoordMode get(String id) {
        for (CoordMode cm : CoordMode.values()) {
            if (cm.name.equalsIgnoreCase(id)) {
                return cm;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }

    public static String getCoordinates(Player p) {
        return p.getLocation().getBlockX() + " " + p.getLocation().getBlockY() + " " + p.getLocation().getBlockZ();
    }
}
