package com.roverisadog.infohud.config;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum DirectionMode {

	DISABLED(0, "disabled", "disabled"),
	SIMPLE(1, "simple", "simple"),
	DETAILED(2, "detailed", "detailed");

	public static final List<String> OPTIONS_LIST = Arrays.stream(DirectionMode.values())
			.map(DirectionMode::toString)
			.collect(Collectors.toList());
	public static final String cmdName = "direction";
	public static final String cfgKey = "directionMode";

	public final int id;
	public final String name;
	public final String description;

	DirectionMode(int id, String name, String description) {
		this.id = id;
		this.name = name;
		this.description = description;
	}

	/**
	 * Gets the direction mode enum value from a name.
	 * @param name Name of the direction mode.
	 * @return Enum value.
	 * @throws NullPointerException If unknown name.
	 */
	public static DirectionMode get(String name) throws NullPointerException {
		for (DirectionMode dm : DirectionMode.values()) {
			if (dm.name.equalsIgnoreCase(name)) {
				return dm;
			}
		}
		throw new NullPointerException();
	}

	/**
	 * Gets the string representation of the direction mode.
	 * @return Name.
	 */
	@Override
	public String toString() {
		return name;
	}

	/**
	 * Get a string representing the cardinal direction the player is facing.
	 * @param player Player.
	 * @return String representing the player's facing direction.
	 */
	public static String getCardinalDirection(Player player) {
		//-180: Leaning left | +180: Leaning right
		float yaw = player.getLocation().getYaw();
		//Bring to 360 degrees (Clockwise from -X axis)
		if (yaw < 0.0F) {
			yaw += 360.0F;
		}
		//Separate into 8 sectors (Arc: 45deg), offset by 1/2 sector (Arc: 22.5deg)
		int sector = (int) ((yaw + 22.5F) / 45.0F);
		switch (sector) {
			case 1: return "SW";
			case 2: return "W [-X]";
			case 3: return "NW";
			case 4: return "N [-Z]";
			case 5: return "NE";
			case 6: return "E [+X]";
			case 7: return "SE";
			case 0:
			default: //Example: (359 + 22.5)/45
				return "S [+Z]";
		}
	}

	/**
	 * Get a string representing the yaw and pitch of the player's gaze in degrees.
	 * @param player Player.
	 * @return String representing the player's yaw and pitch.
	 */
	public static String getAngularDirection(Player player) {
		Location loc = player.getLocation();
		return String.format("(%.1f / %.1f)", loc.getYaw(), loc.getPitch());
	}
}
