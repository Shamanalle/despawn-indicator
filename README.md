<div align="center">

<img src="docs/images/icon.png" width="128" alt="Despawn Indicator icon: a diamond inside a countdown ring">

# Despawn Indicator

[![Minecraft 1.20.1 – 26.3](https://img.shields.io/badge/Minecraft-1.20.1%20%E2%80%93%2026.3-62b47a)](#versions-and-files)
[![Fabric](https://img.shields.io/badge/loader-Fabric-dbd0b4)](https://fabricmc.net/)
[![Quilt](https://img.shields.io/badge/loader-Quilt-8e4dd0)](https://quiltmc.org/)
[![NeoForge](https://img.shields.io/badge/loader-NeoForge-e68c37)](https://neoforged.net/)
[![Forge](https://img.shields.io/badge/loader-Forge-2b2b33)](https://files.minecraftforge.net/)
[![Release](https://img.shields.io/github/v/release/Shamanalle/despawn-indicator?color=4c8bf5&include_prereleases)](https://github.com/Shamanalle/despawn-indicator/releases)
[![In-game tests](https://github.com/Shamanalle/despawn-indicator/actions/workflows/ingame.yml/badge.svg)](https://github.com/Shamanalle/despawn-indicator/actions/workflows/ingame.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-lightgrey)](LICENSE)

**English** · [Русский](README.ru.md)

</div>

Items on the ground disappear after five minutes, and Minecraft never tells you how long one has left. Despawn Indicator does: dropped items start to blink before they despawn, faster and faster toward the end, and show the time left (`0:42`) when you look at them.

> **Work in progress.** The first release, 0.1.0, is not out yet. Already working: the server sends every item's exact despawn time to players with the mod, and `/despawnindicator` shows the mod's status. Coming in 0.1.0: blinking and the time label. Later: a HUD for the items you dropped on death, the Paper / Folia / Spigot plugin and more Minecraft versions. See the [plan](docs/PLAN.md).

## Contents

- [Quick start](#quick-start)
- [What it does](#what-it-does)
- [What works where](#what-works-where)
- [Versions and files](#versions-and-files)
- [Compatibility](#compatibility)
- [Commands](#commands)
- [Questions and problems](#questions-and-problems)
- [Building](#building)
- [License](#license)

## Quick start

**Player**
1. Install [Fabric Loader](https://fabricmc.net/use/) and [Fabric API](https://modrinth.com/mod/fabric-api), [NeoForge](https://neoforged.net/) or [Forge](https://files.minecraftforge.net/).
2. Put the mod's `.jar` into `.minecraft/mods/`. Which file: [Versions and files](#versions-and-files).

That is enough for singleplayer and for any server. On a server without the mod the times are estimated (marked `≈`, see below).

**Server owner**
- Fabric, Forge, NeoForge: put the same `.jar` into the server's `mods/`. Players with the mod then see exact times; players without it are not affected and can still join.
- Paper, Folia, Spigot: a plugin is planned.

## What it does

- **Blinking:** an item blinks during its last 20 seconds, faster toward the end. It is drawn by skipping frames rather than by transparency, so it looks the same with shaders.
- **Time label:** `0:42` above an item when you look at it or sneak near it. Items that never despawn show nothing.
- **Exact or estimated:** with the mod on the server (or in singleplayer) every time is exact. Without it the client starts counting when it first sees an item and marks the time `≈`: an item that was lying there before you came looks younger than it is, so it may vanish while the label still shows time left.
- Server settings are respected: Forge and NeoForge item lifespans, items that never despawn, and (with the plugin) Paper's and Spigot's despawn rates.

## What works where

| | Client only | Client + server mod | Singleplayer |
|---|---|---|---|
| Blinking and time label | ✅ estimated `≈` | ✅ exact | ✅ exact |
| Items dropped before you came near | ⚠️ look younger than they are | ✅ | ✅ |
| Forge / NeoForge item lifespans, items that never despawn | ❌ assumed 5:00 | ✅ | ✅ |
| `/despawnindicator` status | — | ✅ | ✅ |

The server half sends only numbers: the item's id, its age and its lifespan, when an item comes into view and when it changes. Players without the mod receive nothing.

## Versions and files

| File | Loaders | Minecraft | Java |
|---|---|---|---|
| `despawn-indicator-fabric-<version>+mc1.21-26.x.jar` | Fabric, Quilt (needs Fabric API) | 1.21 – 1.21.11, 26.1 – 26.3 | 21 (26.x: 25) |
| `despawn-indicator-forge-neoforge-<version>+mc1.20.1-1.21.1.jar` | Forge 1.20.1, NeoForge 1.20.1 and 1.21 – 1.21.1 | 1.20.1, 1.21 – 1.21.1 | 17 (1.21: 21) |

Each file holds a build for every Minecraft version in its range and picks the right one at startup. On Forge the second file runs on 1.20.1 only; Modrinth and CurseForge also list Forge 1.21 for it, because they take one list of versions per file. More versions (1.16.5 and up) will follow.

## Compatibility

- **Shaders (Iris, Oculus), Sodium, Embeddium:** blinking skips frames instead of changing transparency, so shader packs draw it correctly.
- **Grave mods:** when a grave mod stores your items in a grave block, there are no items on the ground and nothing to count down.
- **Item clearing mods** (ClearLag and the like) remove items on their own schedule, which the countdown cannot know.

## Commands

`/despawnindicator`: the mod's version, how many online players have it and how long dropped items stay on this server. Answers in the player's language (on Forge 1.20.1 in English). Anyone can run it, also from the server console.

## Questions and problems

**The time has `≈`.** The server does not have the mod, so the time is counted from when you first saw the item.

**An item vanished before its time ran out.** Something other than despawning removed it: a player, a hopper, lava, a cactus, or a clearing mod or plugin.

Bugs and ideas: [issues](https://github.com/Shamanalle/despawn-indicator/issues).

## Building

JDK 25 builds every target: `./gradlew build`, jars in `build/libs/`. Details in [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[MIT](LICENSE).
