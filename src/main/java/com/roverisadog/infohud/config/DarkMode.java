package com.roverisadog.infohud.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum DarkMode {

	DISABLED(0, "disabled", "always disabled"),
	ENABLED(1, "enabled", "always enabled"),
	AUTO(2, "auto", "automatic");

	public static final List<String> OPTIONS_LIST = Arrays.stream(DarkMode.values())
			.map(DarkMode::toString)
			.collect(Collectors.toList());
	public static final String cmdName = "darkMode";
	public static final String cfgKey = "darkMode";

	public final int id;
	public final String name;
	public final String description;

	DarkMode(int id, String name, String description) {
		this.id = id;
		this.name = name;
		this.description = description;
	}

	/**
	 * Get the dark mode setting enum value corresponding to a numerical ID.
	 * 0: Disabled, 1: Enabled, 2: Auto.
	 * Only to be used to parse older versions of config.yml.
	 * @param id Numerical ID for the dark mode setting.
	 * @return Enum value.
	 * @throws NullPointerException If unknown ID.
	 */
	@Deprecated
	public static DarkMode get(int id) throws NullPointerException {
		for (DarkMode dm : DarkMode.values()) {
			if (dm.id == id) {
				return dm;
			}
		}
		throw new NullPointerException();
	}

	/**
	 * Gets the dark mode setting enum value from a name.
	 * @param name Name of the dark mode setting.
	 * @return Enum value.
	 * @throws NullPointerException If unknown name.
	 */
	public static DarkMode get(String name) throws NullPointerException {
		for (DarkMode dm : DarkMode.values()) {
			if (dm.name.equalsIgnoreCase(name)) {
				return dm;
			}
		}
		throw new NullPointerException();
	}

	/**
	 * Gets the string representation of the dark mode setting.
	 * @return Name.
	 */
	@Override
	public String toString() {
		return name;
	}
}
