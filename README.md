<p align="center"><img src="/img/thumbnailRect.png"></p>

## InfoHUD
Display coordinates and time to the player's action bar. All the functionality of <a href="https://vanillatweaks.net/picker/datapacks/.">Coordinates HUD</a> and more! \
Version 1.4, compatible with Craftbukkit/Spigot/Paper 1.8+. Compiled using Spigot 1.9.4.

### Download & Installation

<a href="https://dev.bukkit.org/projects/infohud">Download on curseforge</a> \
<a href="https://www.spigotmc.org/resources/infohud.83844/">Download on spigotmc</a>

Drag `InfoHUD-1.XX.jar` in the plugins folder. The plugin should work for all versions 1.8+.

## Features
Display your current coordinates.
<p align="center"><img src="/img/banner.png"></p>
Display the current time in different formats.
<p align="center"><img src="/img/villagerTime.png"></p>
Automatically switch to dark mode in brighter biomes such as deserts and snow biomes.
<p align="center"><img src="/img/darkMode.png"></p>
Nearly every settings can be adjusted (See config.yml).

## Commands
**Per player (infohud.use):**\
`/infohud <enable|disable>` : Enable/Disable InfoHUD for yourself.\
`/infohud coordinates <disabled|enabled>` : Enable/Disable showing your coordinates.\
`/infohud time <disabled|currentTick|clock12|clock24|villagerSchedule>` : Time display format. \
`/infohud darkMode <disabled|enabled|auto>` : Dark mode settings.

**Global (infohud.admin):**\
`/infohud messageUpdateDelay`: Change how quickly (ticks) the text is being updated.\
`/infohud reload`: Reload settings (Reloads config.yml).\
`/infohud benchmark`: Display how long InfoHUD took to process the last update.\
`/infohud brightBiomes <add|remove> <here|BIOME_NAME>`: Add/Remove biomes where dark mode turns on automatically.

## Permissions
`infohud.use` Allows player to enable/disable InfoHUD and change their own settings.\
`infohud.admin` Allows player to change global settings.

## config.yml
For versions 1.2 and lower, see [here](./README_OLD.md).
```yaml
infohudVersion: '1.X'
# Ticks between each update. Performance cost is extremely small so you are unlikely to run into any
# performance issues even if it is set to 1. Values above 20 can lead to the message fading.
messageUpdateDelay: <number> {Default:5}
# Lower to reduce the delay between entering a bright biome and InfoHUD changing colors. 
# Very heavy performance impact since MC 1.13. Recommend above 20.
biomeUpdateDelay: <number> {Default:40}
# Colors used by the bright and dark modes respectively (UPPERCASE). https://minecraft.gamepedia.com/Formatting_codes
colors:
  bright1: GOLD
  bright2: WHITE
  dark1: DARK_BLUE
  dark2: DARK_AQUA
# Biomes where dark mode will turn on automatically.
# Find at https://minecraft.gamepedia.com/Biome#Biome_IDs, the F3 menu or use /infohud biome add
# Must be in UPPERCASE. Eg. DEEP_FROZEN_OCEAN
# Only biomes in the list recognized by your current MC version will be loaded. Biomes from older/newer versions
# will not be loaded, but remain in the file.
brightBiomes:
- DESERT
- BIOME_NAME
- ...
# Settings on a per-player basis. https://namemc.com/ to get your UUID.
playerConfig:
  7445052d-632b-4aa1-8da8-44be2053bd5b:
    coordinatesMode: <enabled | disabled>
    timeMode: <disabled | currentTick | clock12 | clock24 | villagerSchedule>
    darkMode: <disabled | enabled | auto>
  AnotherUUID:
    coordinatesMode: enabled
    timeMode: clock12
    darkMode: auto
  ...
```

## See also
This plugin is inspired by the excellent CoordinatesHUD datapack. Find it at https://vanillatweaks.net/picker/datapacks/.