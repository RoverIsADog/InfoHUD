# InfoHUD
![alt text](https://github.com/RoverIsADog/InfoHUD/tree/master/img/banner.png "InfoHUD")\
Display coordinates and time to the player action bar.
Inspired by the popular Coordinates HUD datapack. Find it at https://vanillatweaks.net/picker/datapacks/.

## Features
Display your current coordinates.\
Display the current time (ticks/HH:mm) or the current villager schedule (1.14+).\
Automatically switch to dark mode in brighter biomes.

## Usage
**Per player:**\
  `/infohud <enable|disable>`: Enable/Disable InfoHUD for yourself.\
  `/infohud coordinates <disabled|enabled>`: Enable/Disable showing coordinates.\
  ![alt text](https://github.com/RoverIsADog/InfoHUD/tree/master/img/villagerTime.png "Time remaining for villager schedule")\
  `/infohud time <disabled|currentTick|clock|villagerSchedule>`: How time should be displayed.\
  ![alt text](https://github.com/RoverIsADog/InfoHUD/tree/master/img/darkMode.png "Dark mode on snow")\
  `/infohud darkMode <disabled|enabled|auto>`: Dark mode settings.\
**Global:**\
  `/infohud refreshRate`: Change how quickly (ticks) the text is being refreshed. **Under construction**.\
  `/infohud reload`: Reload settings (Reload config.yml). **Under construction**.
## Permissions
`infohud.use` Allows player to enable/disable InfoHUD and change their own settings.\
`infohud.admin` Allows player to change global settings. **Under construction**.

## Installation
Drag `InfoHUD.jar` in your plugins folder.
