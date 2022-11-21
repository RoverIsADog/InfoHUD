package com.roverisadog.infohud.config;

import org.bukkit.Location;
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
	public static final String cmdName = "coordinates";
	public static final String cfgKey = "coordinatesMode";

	public final int id;
	public final String name;
	public final String description;

	CoordMode(int id, String name, String description) {
		this.id = id;
		this.name = name;
		this.description = description;
	}

	/**
	 * Get the coordinates mode enum value corresponding to a numerical ID.
	 * 0: Disabled, 1: Enabled. Only to be used to parse older versions of config.yml.
	 * @param id Numerical ID for the coordinate mode.
	 * @return Enum value.
	 * @throws NullPointerException If unknown ID.
	 */
	@Deprecated
	public static CoordMode get(int id) throws NullPointerException {
		for (CoordMode cm : CoordMode.values()) {
			if (cm.id == id) {
				return cm;
			}
		}
		throw new NullPointerException();
	}

	/**
	 * Gets the coordinates mode enum value from a name.
	 * @param name Name of the coordinate mode.
	 * @return Enum value.
	 * @throws NullPointerException If unknown name.
	 */
	public static CoordMode get(String name) throws NullPointerException {
		for (CoordMode cm : CoordMode.values()) {
			if (cm.name.equalsIgnoreCase(name)) {
				return cm;
			}
		}
		throw new NullPointerException();
	}

	/**
	 * Gets the string representation of the coord mode.
	 * @return Name.
	 */
	@Override
	public String toString() {
		return name;
	}

	/**
	 * Helper method to get the string representation of a player's coordinates.
	 * @param p Player.
	 * @return
	 */
	public static String getCoordinates(Player p) {
		Location loc = p.getLocation();
		return String.format("%s %s %s", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
	}
}
