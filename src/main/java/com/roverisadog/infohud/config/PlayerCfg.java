package com.roverisadog.infohud.config;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Contains the InfoHUD configuration for one player.
 */
public class PlayerCfg {

	/** UUID of the player. */
	private final UUID id;

	/** (Persistent) How coordinates should be displayed (enum in case more modes added.) */
	private CoordMode coordMode;
	/** (Persistent) Direction mode setting. */
	private DirectionMode directionMode;
	/** (Persistent) Format to display the time in. */
	private TimeMode timeMode;
	/** (Persistent) Dark mode settings. */
	private DarkMode darkMode;

	/** (Cache) Tracks whether the player is currently in a "bright biome" to avoid
	 * expensively checking every tick. See biomeUpdateDelay in config.yml */
	private boolean isInBrightBiome = false;

	/** (Cache) Tracks whether the plugin should be paused for this player (> 0) and for how
	 * long. */
	private int pausedFor = 0;

	/**
	 * Constructor that creates default configs for a player:
	 * <ul>
	 *     <li>coordinatesMode: enabled</li>
	 *     <li>directionMode: simple</li>
	 *     <li>timeMode: clock24</li>
	 *     <li>darkMode: auto</li>
	 * </ul>
	 * @param id UUID of the player.
	 */
	protected PlayerCfg(UUID id) {
		this(id, CoordMode.ENABLED, DirectionMode.SIMPLE, TimeMode.CLOCK24, DarkMode.AUTO);
	}

	protected PlayerCfg(UUID id, CoordMode coordMode, DirectionMode directionMode, TimeMode timeMode, DarkMode darkMode) {
		this.id = id;
		this.coordMode = coordMode;
		this.directionMode = directionMode;
		this.timeMode = timeMode;
		this.darkMode = darkMode;
	}

	public UUID getId() {
		return id;
	}

	public CoordMode getCoordMode() {
		return coordMode;
	}

	public synchronized void setCoordMode(CoordMode coordMode) {
		this.coordMode = coordMode;
	}

	public DirectionMode getDirectionMode() {
		return directionMode;
	}

	public synchronized void setDirectionMode(DirectionMode directionMode) {
		this.directionMode = directionMode;
	}

	public TimeMode getTimeMode() {
		return timeMode;
	}

	public synchronized void setTimeMode(TimeMode timeMode) {
		this.timeMode = timeMode;
	}

	public DarkMode getDarkMode() {
		return darkMode;
	}

	public synchronized void setDarkMode(DarkMode darkMode) {
		this.darkMode = darkMode;
	}

	public boolean isInBrightBiome() {
		return isInBrightBiome;
	}

	public synchronized void setInBrightBiome(boolean inBrightBiome) {
		isInBrightBiome = inBrightBiome;
	}

	public synchronized void pauseFor(int time) {
		if (time < pausedFor) return;
		pausedFor = time;
	}

	/**
	 * Checks whether InfoHUD should be paused for the given player.
	 * @return True if not paused.
	 */
	protected synchronized boolean updateAndCheckPaused(int ticks) {
		if (pausedFor > 0) pausedFor -= ticks;
		return pausedFor <= 0;
	}

	/**
	 * Used to convert a player's configuration from InfoHUD internal representation into
	 * a mapping that CraftBukkit's KV store recognises.
	 * Basically the inverse of {@link #fromRawMap(UUID, Map)}
	 * @return Mapping that is recognized by the bukkit yaml configuration.
	 */
	protected Map<String, Object> toRawMap() {
		Map<String, Object> tmp = new HashMap<>();
		tmp.put(CoordMode.cfgKey, coordMode.toString());
		tmp.put(DirectionMode.cfgKey, directionMode.toString());
		tmp.put(TimeMode.cfgKey, timeMode.toString());
		tmp.put(DarkMode.cfgKey, darkMode.toString());
		return tmp;
	}

	/**
	 * Loads a player's raw configuration (loaded from config.yml) into a PlayerCfg object.
	 * Basically the inverse of {@link #toRawMap()}. Can handle old config.yml versions.
	 * @param map Map to read from.
	 * @return PlayerCfg containing the desired values.
	 */
	@SuppressWarnings("deprecation")
	public static PlayerCfg fromRawMap(UUID id, Map<String, Object> map) {

		CoordMode coordMode;
		DirectionMode directionMode;
		TimeMode timeMode;
		DarkMode darkMode;

		//Is from a version before InfoHUD 1.2 (Stored as int)
		if (map.get(CoordMode.cfgKey) instanceof Integer) {
			coordMode = CoordMode.get((int) map.get(CoordMode.cfgKey));
			directionMode = DirectionMode.SIMPLE;
			timeMode = TimeMode.get((int) map.get(TimeMode.cfgKey));
			darkMode = DarkMode.get((int) map.get(DarkMode.cfgKey));
		}
		//Is from newer versions (stored as String)
		else {
			coordMode = CoordMode.get((String) map.get(CoordMode.cfgKey));
			directionMode = DirectionMode.get((String) map.getOrDefault(DirectionMode.cfgKey, DirectionMode.SIMPLE.name));
			timeMode = TimeMode.get((String) map.get(TimeMode.cfgKey));
			darkMode = DarkMode.get((String) map.get(DarkMode.cfgKey));
		}

		return new PlayerCfg(id, coordMode, directionMode, timeMode, darkMode);
	}

}
