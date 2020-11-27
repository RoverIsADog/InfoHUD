package com.roverisadog.infohud.command;

import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum CoordMode {

    DISABLED(0,
            "disabled",
            (p, col1, col2) -> ""),
    ENABLED(1,
            "enabled",
            (p, col1, col2) -> p.getLocation().getBlockX() + " " + p.getLocation().getBlockY() + " " + p.getLocation().getBlockZ());


    public static List<String> optionsList = Arrays.stream(CoordMode.values())
            .map(CoordMode::name)
            .collect(Collectors.toList());
    public static String cmdName = "coordinates";
    public static String cfgKey = "coordinatesMode";

    public String name;
    public int id;
    private StringCreator stringCreator;

    CoordMode(int id, String name, StringCreator stringCreator) {
        this.id = id;
        this.name = name;
        this.stringCreator = stringCreator;
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

    public String toString(Player p, String col1, String col2) {
        return stringCreator.getString(p, col1, col2);
    }

}
