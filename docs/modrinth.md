Items on the ground vanish after five minutes, and Minecraft never says how long one has left. **Despawn Indicator** does.

- **Blinking:** a dropped item blinks during its last 20 seconds, faster toward the end. The blink skips frames instead of fading, so it looks right with shaders.
- **Time left:** look at an item, or sneak near it, to see `0:42` above it. Items that never despawn show nothing.
- **Works anywhere:** on a server without the mod the time is an estimate, marked `≈`. Put the same file on the server (Fabric, Forge, NeoForge), or the plugin on Paper, Folia or Spigot, and every time is exact, including item lifespans changed by other mods. Players without the mod can still join.
- **After death:** a timer in the corner counts down until your dropped items vanish, with an arrow, the distance and the coordinates. It turns yellow in the last minute and blinks red in the last 15 seconds, and goes away once you pick everything up.
- **`/despawnindicator`** tells a server owner how many online players have the mod.

![Dropped items with the time each has left above them](https://raw.githubusercontent.com/Shamanalle/despawn-indicator/main/docs/images/timers.png)

Three files cover everything: one for Fabric and Quilt (Minecraft 1.16.5 – 1.21.11 and 26.1 – 26.3, needs Fabric API), one for Forge 1.20.1 and NeoForge 1.20.1 – 26.3, and a server plugin for Paper, Folia and Spigot 1.16.5 – 26.3.

[Source and details](https://github.com/Shamanalle/despawn-indicator) · [Bugs and ideas](https://github.com/Shamanalle/despawn-indicator/issues)

<details>
<summary>Русский</summary>

Предметы на земле исчезают через пять минут, и Minecraft никак не показывает, сколько им осталось. **Despawn Indicator** показывает.

- **Мигание:** выброшенный предмет мигает последние 20 секунд, к концу всё чаще. Мигание пропускает кадры, а не меняет прозрачность, поэтому правильно выглядит с шейдерами.
- **Оставшееся время:** посмотрите на предмет или подкрадитесь к нему, и над ним появится `0:42`. Над предметами, которые не исчезают, ничего нет.
- **Работает везде:** на сервере без мода время приблизительное и помечено `≈`. Положите тот же файл на сервер (Fabric, Forge, NeoForge) или плагин на Paper, Folia или Spigot, и время станет точным, в том числе если другие моды меняют время жизни предметов. Игроки без мода по-прежнему могут заходить.
- **После смерти:** таймер в углу экрана считает, сколько осталось вашим выпавшим вещам, и показывает стрелку, расстояние и координаты. В последнюю минуту он жёлтый, в последние 15 секунд мигает красным и пропадает, когда вы всё подобрали.
- **`/despawnindicator`** показывает владельцу сервера, сколько игроков онлайн с модом.

Всего три файла: для Fabric и Quilt (Minecraft 1.16.5 – 1.21.11 и 26.1 – 26.3, нужен Fabric API), для Forge 1.20.1 и NeoForge 1.20.1 – 26.3 и плагин для серверов Paper, Folia и Spigot 1.16.5 – 26.3.

[Исходники и подробности](https://github.com/Shamanalle/despawn-indicator) · [Ошибки и идеи](https://github.com/Shamanalle/despawn-indicator/issues)

</details>
