# Changelog

All notable changes to this project are documented in this file. Each version is described in English first, then in Russian.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

*Все заметные изменения проекта описываются в этом файле. Каждая версия описана сначала на английском, затем на русском.*

## [Unreleased]

### English

#### Added
- After you die, a timer in the top left corner counts down until your dropped items despawn and shows the way back: an arrow, the distance, the coordinates and the world. It turns yellow in the last minute, blinks red in the last 15 seconds and goes away once you pick the items up. With the mod on the server it is exact and pauses while nobody is near your items; on other servers it is an estimate marked `≈`. Nothing appears when a grave or keepInventory saved your items.
- A settings file for players, `config/despawnindicator-client.properties`: turn the death timer off, choose how many seconds before despawning items start to warn, swap blinking for a time label, choose when labels show, and turn on a sound for the death timer (off by default). Changes apply without a restart.
- Server owners can let players see exact times only for their own items, so nobody learns when someone else's loot vanishes: `config/despawnindicator-server.properties` on the server.
- A plugin for Paper, Folia and Spigot servers, Minecraft 1.16.5 and newer: put it into `plugins/` and players with the mod get exact item times and the exact death timer, the same as with the mod on the server. Players without the mod can still join. `/despawnindicator` works there too, and the server settings file is `plugins/DespawnIndicator/despawnindicator-server.properties`.
- A fading style: instead of blinking, items shrink and grow back during their last seconds, faster toward the end, and never vanish completely; the death timer fades in and out. It works with shaders. Turn it on with `style=fade` in `config/despawnindicator-client.properties`.

### Русский

#### Добавлено
- После смерти таймер в левом верхнем углу отсчитывает время до исчезновения выпавших вещей и показывает, куда за ними идти: стрелка, расстояние, координаты и мир. В последнюю минуту он жёлтый, в последние 15 секунд мигает красным, а когда вы подберёте вещи, пропадает. С модом на сервере время точное и стоит на паузе, пока рядом с вещами никого нет; на других серверах оно приблизительное и помечено `≈`. Если вещи сохранила могила или keepInventory, таймера нет.
- Файл настроек для игроков, `config/despawnindicator-client.properties`: можно выключить таймер после смерти, выбрать, за сколько секунд до исчезновения предметы начинают предупреждать, заменить мигание подписью со временем, выбрать, когда показываются подписи, и включить звук для таймера после смерти (по умолчанию выключен). Изменения применяются без перезапуска.
- Владелец сервера может показывать игрокам точное время только их собственных предметов, чтобы никто не узнавал, когда исчезнут чужие вещи: `config/despawnindicator-server.properties` на сервере.
- Плагин для серверов Paper, Folia и Spigot, Minecraft 1.16.5 и новее: положите его в `plugins/`, и игроки с модом получат точное время предметов и точный таймер после смерти, как с модом на сервере. Игроки без мода по-прежнему могут заходить. `/despawnindicator` там тоже работает, а файл настроек сервера — `plugins/DespawnIndicator/despawnindicator-server.properties`.
- Стиль с затуханием: вместо мигания предметы в последние секунды уменьшаются и снова растут, к концу всё чаще, но совсем не пропадают; таймер после смерти плавно гаснет и появляется. Работает с шейдерами. Включается строкой `style=fade` в `config/despawnindicator-client.properties`.

## [0.1.0] - 2026-10-22

### English

#### Added
- Dropped items blink during their last 20 seconds, faster toward the end. It looks the same with shaders.
- Look at a dropped item to see how long it has left (`0:42`) above it; sneak to see it above every item around you. Items that never despawn show nothing.
- Works on any server. Without the mod on the server the time is counted from when you first see an item and is marked `≈`, because an item that was already lying there is older than it looks.
- With the mod on the server, players who have it learn exactly when each dropped item near them will despawn, including items that never despawn and the item lifespans set by Forge and NeoForge mods. Players without the mod can still join.
- **`/despawnindicator`** shows the mod's version, how many online players have it and how long dropped items stay on the server. Anyone can run it, also from the server console.

### Русский

#### Добавлено
- Выброшенные предметы мигают последние 20 секунд, к концу всё чаще. С шейдерами выглядит так же.
- Посмотрите на выброшенный предмет, и над ним появится оставшееся время (`0:42`); если красться, время видно над всеми предметами вокруг. Над предметами, которые никогда не исчезают, ничего нет.
- Работает на любом сервере. Без мода на сервере время считается с момента, когда вы впервые увидели предмет, и помечено `≈`: предмет, который уже лежал, старше, чем кажется.
- Если мод стоит на сервере, игроки с модом точно знают, когда исчезнет каждый выброшенный предмет рядом с ними, в том числе предметы, которые не исчезают никогда, и время жизни, заданное модами Forge и NeoForge. Игроки без мода по-прежнему могут заходить.
- **`/despawnindicator`** показывает версию мода, сколько игроков онлайн с модом и сколько лежат выброшенные предметы на сервере. Доступна всем, в том числе из консоли сервера.
