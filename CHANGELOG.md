# Changelog

All notable changes to this project are documented in this file. Each version is described in English first, then in Russian.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

*Все заметные изменения проекта описываются в этом файле. Каждая версия описана сначала на английском, затем на русском.*

## [Unreleased]

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
