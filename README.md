# InfoHUD
![banner](/img/banner.png) \
Display coordinates and time to the player action bar. Inspired by the popular Coordinates HUD datapack. Find it at https://vanillatweaks.net/picker/datapacks/. \
Version **1.0**.

## Features
Display your current coordinates.
<p align="center"><img src="/img/banner.png"></p>
Display the current tick, current time (ticks/HH:mm) or the current villager schedule (1.14+).
<p align="center"><img src="/img/villagerTime.png"></p>
Automatically switch to dark mode in brighter biomes.
<p align="center"><img src="/img/darkMode.png"></p>

## Installation
Drag `InfoHUD.jar` in your plugins folder.
The plugin should work for all versions 1.8+.

## Commands
**Per player:**\
`/infohud <enable|disable>`: Enable/Disable InfoHUD for yourself.\
`/infohud coordinates <disabled|enabled>`: Enable/Disable showing coordinates.\
`/infohud time <disabled|currentTick|clock|villagerSchedule>`: How time should be displayed. Villager schedule only available 1.14+.\
`/infohud darkMode <disabled|enabled|auto>`: Dark mode settings.\

**Global:**\
`/infohud refreshRate`: Change how quickly (ticks) the text is being refreshed.\
`/infohud reload`: Reload settings (Reload config.yml).\
`/infohud benchmark`: Display how long InfoHUD took to process the last update.\
`/infohud brightBiomes <add|remove> <here|BIOME_NAME>`: Add/Remove biomes where dark mode turns on automatically.

## Permissions
`infohud.use` Allows player to enable/disable InfoHUD and change their own settings.\
`infohud.admin` Allows player to change global settings.

## config.yml
```yaml
#Find the list of biomes at https://minecraft.gamepedia.com/Biome#Biome_IDs.
#Alternatively enter the biome name as it appears in the F3 menu, or use /infohud biome add
#Must be in UPPERCASE. Eg. DEEP_FROZEN_OCEAN
brightBiomes: #Biomes where dark mode will turn on
- DESERT
- BIOME_NAME
- ...

#Interval in ticks between each refresh. Lower for better performance.
refreshRate: <number> {Default:5}

#Settings on a per-player basis
playerConfig:
  UUID: #The player's UUID
    coordinatesMode: <number> {0:Enabled | 1:Disabled}
    timeMode: <number> {0:Disabled | 1:Current Tick | 2:24h Clock | 3: 1.14 Villager Schedule}
    darkMode: <number> {0:Disabled | 1:Enabled | 2:Auto}
  AnotherUUID:
    coordinatesMode: 1
    timeMode: 2
    darkMode: 2
  ...
```
