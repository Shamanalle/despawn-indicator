<div align="center">

<img src="docs/images/icon.png" width="128" alt="Despawn Indicator icon: a diamond inside a countdown ring">

# Despawn Indicator

[![Minecraft 1.17.1 – 26.3](https://img.shields.io/badge/Minecraft-1.17.1%20%E2%80%93%2026.3-62b47a)](#versions-and-files)
[![Fabric](https://img.shields.io/badge/loader-Fabric-dbd0b4)](https://fabricmc.net/)
[![Quilt](https://img.shields.io/badge/loader-Quilt-8e4dd0)](https://quiltmc.org/)
[![NeoForge](https://img.shields.io/badge/loader-NeoForge-e68c37)](https://neoforged.net/)
[![Forge](https://img.shields.io/badge/loader-Forge-2b2b33)](https://files.minecraftforge.net/)
[![Paper / Folia / Spigot plugin](https://img.shields.io/badge/plugin-Paper%20%2F%20Folia%20%2F%20Spigot%201.16.5%2B-3b7bbf)](#versions-and-files)
[![Release](https://img.shields.io/github/v/release/Shamanalle/despawn-indicator?color=4c8bf5&include_prereleases)](https://github.com/Shamanalle/despawn-indicator/releases)
[![In-game tests](https://github.com/Shamanalle/despawn-indicator/actions/workflows/ingame.yml/badge.svg)](https://github.com/Shamanalle/despawn-indicator/actions/workflows/ingame.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-lightgrey)](LICENSE)

**English** · [Русский](README.ru.md)

</div>

Items on the ground disappear after five minutes, and Minecraft never tells you how long one has left. Despawn Indicator does: dropped items start to blink before they despawn, faster and faster toward the end, and show the time left (`0:42`) when you look at them. After you die, a timer on the screen counts down to your lost items and points the way back.

> **Early version.** Blinking, the time label, the death loot timer, exact times with the server mod or the Paper / Folia / Spigot plugin and `/despawnindicator` and a fading style that works with shaders are in. Coming later: more Minecraft versions for the mod. See the [plan](docs/PLAN.md).

## Contents

- [Quick start](#quick-start)
- [What it does](#what-it-does)
- [What works where](#what-works-where)
- [Settings](#settings)
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
- Paper, Folia, Spigot (and forks such as Purpur), 1.16.5 and newer: put the plugin `despawn-indicator-paper-folia-spigot-<version>+mc1.16.5-26.x.jar` into the server's `plugins/` and restart. It does the same as the server mod. Players without the mod are not affected.

## What it does

- **Blinking:** an item blinks during its last 20 seconds, faster toward the end. It is drawn by skipping frames rather than by transparency, so it looks the same with shaders.
- **Fading** (`style=fade`): instead of blinking, an item shrinks and grows back during its last 20 seconds, faster toward the end, but never vanishes. It is only drawn smaller, so this too works with shaders. The death loot timer fades in and out.
- **Time label:** `0:42` above an item when you look at it (up to 24 blocks away), and above every item within 8 blocks while you sneak (the nearest 32). White, yellow in the last minute, red in the last 20 seconds. A labelled item stops blinking so the label can be read. Items that never despawn and items that already have a name show nothing. The label is the item's name tag, set on your client only.
- **Exact or estimated:** with the mod on the server (or in singleplayer) every time is exact. Without it the client starts counting when it first sees an item and marks the time `≈`: an item that was lying there before you came looks younger than it is, so it may vanish while the label still shows time left.
- **Death loot timer:** after you die, the top left corner shows how long until the first of your dropped items despawns (`Death loot despawns in 4:12`), with an arrow, the distance, the coordinates and the world. It turns yellow in the last minute and blinks red in the last 15 seconds, and goes away once you pick the items up or they despawn. With the mod on the server it is exact; it tracks the items that appeared where you died, and if they are scattered it counts down to the one that despawns first. While nobody is near them their clock stops, and the timer says `(paused)`. Hidden with the rest of the HUD (F1).
- Server settings are respected: Forge and NeoForge item lifespans, items that never despawn, and (with the plugin) Paper's and Spigot's despawn rates.

## What works where

| | Client only | Client + server mod or plugin | Singleplayer |
|---|---|---|---|
| Blinking and time label | ✅ estimated `≈` | ✅ exact | ✅ exact |
| Items dropped before you came near | ⚠️ look younger than they are | ✅ | ✅ |
| Forge / NeoForge item lifespans, items that never despawn | ❌ assumed 5:00 | ✅ | ✅ |
| Death loot timer | ✅ estimated `≈` | ✅ exact, pauses with the items | ✅ exact |
| `/despawnindicator` status | — | ✅ | ✅ |

The server half sends only numbers: the item's id, its age and its lifespan, when an item comes into view and when it changes; after a death, where the loot lies and how long it has left. Players without the mod receive nothing.

Without the mod on the server the death loot timer is a guess: a second after you die the client looks for items around the spot and counts their lifespan down from the moment of death. It cannot see the clock stop while you are far away, so the loot may last longer than shown. It notices the items are gone once you come back within 24 blocks of the spot.

## Settings

Both files are plain text, are created on first start with comments, and are read again within a second of saving: no restart needed.

**Player:** `config/despawnindicator-client.properties`

| Setting | Default | What it does |
|---|---|---|
| `death_hud` | `true` | The death loot timer on or off. |
| `warn_seconds` | `20` | How many seconds before despawning an item starts to blink and its label turns red (5 – 120). |
| `style` | `blink` | `blink`; `fade`: items shrink and grow back instead of blinking, the death loot timer fades in and out; `label`: items never blink and show their time in the last seconds instead, the death loot timer only turns red. |
| `labels` | `look_sneak` | When items show their time: `look_sneak` (the item you look at, and all around while sneaking), `look`, `sneak` or `never`. |
| `sound` | `false` | A chime when the death loot timer reaches one minute and a bell at 15 seconds. |

**Server owner:** `config/despawnindicator-server.properties` (with the plugin: `plugins/DespawnIndicator/despawnindicator-server.properties`)

| Setting | Default | What it does |
|---|---|---|
| `clocks` | `all` | `all`: players with the mod get exact times for every item they see. `own`: only for the items they dropped or lost when they died; everything else shows the estimate `≈`, so nobody learns when someone else's loot vanishes. |

## Versions and files

| File | Loaders | Minecraft | Java |
|---|---|---|---|
| `despawn-indicator-fabric-<version>+mc1.17-26.x.jar` | Fabric, Quilt (needs Fabric API) | 1.17.1 – 1.21.11, 26.1 – 26.3 | 16 (1.18.2 and up: 17, 1.20.5 and up: 21, 26.x: 25) |
| `despawn-indicator-forge-neoforge-<version>+mc1.20.1-1.21.1.jar` | Forge 1.20.1, NeoForge 1.20.1 and 1.21 – 1.21.1 | 1.20.1, 1.21 – 1.21.1 | 17 (1.21: 21) |
| `despawn-indicator-paper-folia-spigot-<version>+mc1.16.5-26.x.jar` (server plugin) | Paper, Folia, Spigot, Purpur | 1.16.5 – 1.21.11, 26.1 – 26.3 | what the server needs |

Each file holds a build for every Minecraft version in its range and picks the right one at startup. On Forge the second file runs on 1.20.1 only; Modrinth and CurseForge also list Forge 1.21 for it, because they take one list of versions per file. More versions of the mod (Fabric 1.16.5, more Forge and NeoForge versions) will follow. The plugin is on Modrinth and GitHub only: CurseForge keeps server plugins in a separate kind of project.

## Compatibility

- **Shaders (Iris, Oculus), Sodium, Embeddium:** blinking skips frames and fading draws the item smaller, neither changes transparency, so shader packs draw both correctly.
- **Grave mods:** when a grave mod stores your items in a grave block, there are no items on the ground and nothing to count down: the death loot timer does not appear.
- **Item clearing mods** (ClearLag and the like) remove items on their own schedule, which the countdown cannot know.
- **The plugin** is tested on Paper 1.16.5, 1.17.1, 1.20.4, 1.21.11 and 26.1.2 and on Folia 1.21.11; Spigot uses the same API but is not tested on every release. It follows Paper's per-item despawn rates (`alt-item-despawn-rate`) and Spigot's `item-despawn-rate`. On Folia it sees each item from that item's region thread. After `/reload` players who were online get item times but no death loot timer until they rejoin.
- **Mods that draw dropped items their own way** (replacing Minecraft's item renderer) may cancel the blinking and fading; the time label still works.

## Commands

`/despawnindicator`: the mod's or plugin's version, how many online players have the mod and how long dropped items stay on this server. Answers in the player's language (on Forge 1.20.1 and Fabric 1.17.1 – 1.20.1 in English: these versions do not tell the server the player's language). Anyone can run it, also from the server console.

## Questions and problems

**The time has `≈`.** The server has neither the mod nor the plugin, so the time is counted from when you first saw the item.

**No death loot timer after I died.** Nothing was dropped: keepInventory is on, a grave mod took the items, or they fell into the void or lava. The timer is also off with `death_hud=false`.

**The death loot timer vanished while the items are still there.** With the mod or plugin on the server an item that merged into another stack lying nearby, or went through a portal, no longer counts as your loot.

**An item vanished before its time ran out.** Something other than despawning removed it: a player, a hopper, lava, a cactus, or a clearing mod or plugin.

Bugs and ideas: [issues](https://github.com/Shamanalle/despawn-indicator/issues).

## Building

JDK 25 builds every target: `./gradlew build`, jars in `build/libs/`. Details in [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[MIT](LICENSE).
