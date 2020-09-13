# InfoHUD
![banner](/img/banner.png) \
Display coordinates and time to the player action bar.
Inspired by the popular Coordinates HUD datapack. Find it at https://vanillatweaks.net/picker/datapacks/. \
Version **1.0**.

## Features
Display your current coordinates.\
Display the current time (ticks/HH:mm) or the current villager schedule (1.14+).\
Automatically switch to dark mode in brighter biomes.

## Usage
**Per player:**\
`/infohud <enable|disable>`: Enable/Disable InfoHUD for yourself.\
`/infohud coordinates <disabled|enabled>`: Enable/Disable showing coordinates.\
`/infohud time <disabled|currentTick|clock|villagerSchedule>`: How time should be displayed. Villager schedule only available 1.14+.\
![villagerTime](/img/villagerTime.png "Villagers will wander for 30 more seconds") \
`/infohud darkMode <disabled|enabled|auto>`: Dark mode settings.\
![darkMode](/img/darkMode.png "Dark mode on snow") \
**Global:**\
`/infohud refreshRate`: Change how quickly (ticks) the text is being refreshed.\
`/infohud reload`: Reload settings (Reload config.yml).\
`/infohud benchmark`: Display how long InfoHUD took to process the last update.\
`/infohud brightBiomes <add|remove> <here|BIOME_NAME>`: Add/Remove biomes where dark mode turns on automatically.
## Permissions
`infohud.use` Allows player to enable/disable InfoHUD and change their own settings.\
`infohud.admin` Allows player to change global settings.

## Installation
Drag `InfoHUD.jar` in your plugins folder.
The plugin should work for all versions 1.8+.
