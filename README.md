Updated for Minecraft 1.21.4 on Java 21

Compiled with Gradle 8.11

<h1 align="center">
<img src="src/main/resources/assets/goldsqource/icon.png">
<br>goldsqource
</h1>
 
Kotlin rewrite of the popular mod which adds goldsource-style movement to minecraft using the fabric mod loader (should be compatible with quilt). Requires [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin), depends on [YetAnotherConfigLib](https://modrinth.com/mod/yacl), [Fabric API](https://modrinth.com/mod/fabric-api) and optionally [Mod Menu](https://modrinth.com/mod/modmenu).

This mod works fine if installed only on the client, but in order to prevent fall damage from slowing down the player it must be installed on the server too. Players can toggle the mod individually in the settings, or using the keybind.

### Features
- goldsource-like 100 tickrate airacceleration
- bunnyhop
- goldsource ladder movement
- in-game config
- keybind to toggle the mod

### Build Instructions
1. Clone the git repository
2. Navigate into the cloned directory
3. Run `./gradlew build`

The built jar file will be in `build/libs/`

### Credits
- [Полина](https://github.com/polina4096) for [porting to kotlin](https://github.com/polina4096/resquake)
- [Tlesis](https://github.com/Tlesis) and [LeviOP](https://github.com/LeviOP) for [SquakePlusPlus fork](https://github.com/Tlesis/SquakePlusPlus)
- [He11crow](https://github.com/He11crow) for [porting](https://github.com/He11crow/SquakeFabric) the original to fabric [(Modrinth)](https://modrinth.com/mod/squakefabric)
- [squeek502](https://github.com/squeek502) for the [original](https://github.com/squeek502/Squake) squake forge mod [(CurseForge)](https://www.curseforge.com/minecraft/mc-mods/squake)

---

### Unlicense
Released into the public domain.
