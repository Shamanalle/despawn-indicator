# Plan / План

**[English](#english)** · **[Русский](#русский)**

---

## English

### Modules

| Module | What it is |
|---|---|
| `common/` | Plain Java 17, no Minecraft: the wire protocol (`HelloMessage`, `ClockMessage`), the client countdown (`ClientClocks`), who gets which update (`ClockSender`), blink timing (`Blink`), time text (`TimeText`), server texts in 13 languages. Unit-tested. |
| `common-j8/` | The same sources compiled to Java 8 bytecode with Jabel, for the 1.16.5 – 1.17 bands and the plugin later. |
| `shared/mc/` | Minecraft code that is the same on every loader: `ServerClocks` (tracks the items each player can see and queues updates), `StatusCommand`, `ClientTicker`. |
| `shared/fabric/` | Fabric entrypoints and events. |
| `versions/<loader>/<band>/` | One Gradle project per loader and Minecraft range: Fabric 1.21 – 1.21.11, Fabric 26.1 – 26.3, Forge 1.20.1 (+ NeoForge 1.20.1), NeoForge 1.21 – 1.21.1. |
| `universal/`, `packaging/` | Dispatchers and `MergeJars`: the bands are merged into **two jars** (Fabric/Quilt, Forge/NeoForge) that pick the right band at startup. |
| `shared/gametest/` | Fabric client gametest: drops items in a real world, checks the times the client got, takes screenshots. |
| `plugin/` (later) | Paper / Folia / Spigot plugin speaking the same protocol. |

### Phases

1. **Skeleton** (this PR): the modules above, the protocol and the server half on all four priority targets, `/despawnindicator`, CI (build, oldest loaders, 26.x link check, server smoke on 12 loader/version pairs, client gametest under xvfb), release and store workflows, README / CONTRIBUTING / CHANGELOG.
2. **MVP → 0.1.0**: blinking in the last 20 s (frame skip), a time label when you look at an item or sneak near it, the client-only estimate (`≈`), exact times in singleplayer and with the server mod; gametest screenshots of both; release on Modrinth and CurseForge.
3. **Death loot HUD** (done): after you die, a timer in the top left with the time until the first of your drops despawns, an arrow, the distance, the coordinates and the dimension; yellow in the last minute, blinking red in the last 15 s, gone once the items are picked up or despawn. A server with the mod tracks the drops (`DeathLoot`, protocol 2) and sends updates only when the client's count would drift; without it the client guesses (`≈`). Client settings (warning time, style, when labels show, death HUD, sound) and a server option (clocks for all items or only your own) live in `.properties` files under `config/`. Gametest: dies, respawns 30 blocks away, checks the timer and that it goes when the items are picked up.
4. **Plugin** for Paper 1.16.5+, Folia, Spigot: the same hello/clocks channels; reads Paper's `alt-item-despawn-rate` (per item type, through reflection, because the config API differs between versions) and Spigot's `item-despawn-rate`.
5. **Shader-safe fade** as an option next to the frame-skip blink; checked with Iris/Oculus.
6. **More versions**: 1.16.5, 1.17.1, 1.18.2, 1.19.2/1.19.4, 1.20.4 – 1.20.6 and the rest of 26.x as they come out.
7. **Store pages**: gallery from the gametest screenshots, CurseForge page.

### MVP goals (0.1.0)

- Players see which drops are about to vanish and how long any drop has left.
- Works **client-only** on any server; exact with the mod or plugin on the server; never breaks vanilla clients joining a modded server.
- Server cost stays small: updates only for items a player can see, only to players with the mod, batched once per tick, a few bytes per item.
- Looks right with shaders.

### Taken from voice-physics and MultiClicker

- **Bands and universal jars** from voice-physics: per-range Gradle projects, `MergeJars`, `BandAdapter` for Fabric and the Forge / NeoForge dispatchers, Jabel for Java 8 bytecode.
- **CI** from voice-physics: Build & Verify with the oldest loaders, the 26.x link check, In-game tests (server smoke + client gametest under xvfb with Mesa), network retry, release from `CHANGELOG.md`, Modrinth / CurseForge publishing that skips a store while its id or token is missing, the Modrinth page and repository *about* workflows.
- **Docs conventions** from both: README in two files with the same sections, CHANGELOG and CONTRIBUTING English then Russian, bilingual issue and PR templates.
- **From MultiClicker:** a small mod with a client and an optional server half, and a status command that tells a server owner who has the mod.

### Done differently, and why

- **Raw bytes instead of loader payload codecs.** The mod on four loaders and the future plugin must write the same bytes; a plain VarInt format in `common` is the one place that defines them, and `ClockMessageTest` pins it down. A `hello` with a protocol number lets the server talk only to clients that understand it.
- **Blink by skipping frames, not by transparency.** Translucent item rendering is what shader packs break most often; not drawing the item on some frames looks the same with or without shaders. A fade can come later as an option.
- **Two jars from the first release**, not one per version: fewer files for players to choose from and one upload per loader family.
- **13 languages from the start**, with a test that keeps keys and placeholders in step, because the texts are few and short.
- **Client-side estimate is marked `≈`.** Competitors either show nothing without a server mod or show a guess as if it were exact.
- **Settings in a `.properties` file, no settings screen yet.** The file is read again when it changes, so edits apply in game; a screen needs a GUI per loader and version, and Mod Menu only covers Fabric. The death timer guess is also marked `≈`.
- **The `fade` style fades the death timer only;** items still blink until the shader-safe fade (phase 5) exists.
- **Screens are drawn through our own `Canvas`.** `GuiGraphics` changed between 1.20.1, 1.21 and 26.x (26.x builds render state instead of drawing); each band has a small `GuiCanvas`, and `LootOverlay` in `shared/mc` stays the same everywhere.

### Competitors (Modrinth, CurseForge)

ClearDespawn, ClearDespawn Reborn, Despawn Notifier, FadingLoot, Item Despawn Timer, DelTime, DespawnCountdown; plugins itemdespawntimer and bItemDespawn. None of them shares one protocol between the mod and a plugin, follows Paper's `alt-item-despawn-rate`, blinks safely under shaders and has a death loot HUD with an arrow; that set is this mod's place.

### Risks

- **1.21.x API drift.** One Fabric jar runs on 1.21 – 1.21.11; methods whose signature changed between releases (`Entity.level()`, `Entity.position()`, `GameProfile.getName()`) must be avoided. Server smoke covers 1.21, 1.21.1, 1.21.5, 1.21.8, 1.21.11.
- **Forge 1.20.1 has no player language on the server**, so `/despawnindicator` answers in English there.
- **The publish list includes Forge 1.21** for the Forge/NeoForge file, because the stores take one list of game versions per file and NeoForge needs 1.21 – 1.21.1. The jar declares no Forge 1.21 support and does not load there; this is said in the README and can be split into two uploads later.
- **The client-only estimate** is wrong for items that were already lying there; the `≈` mark and the README say so.
- **Blinking replaces the item renderer** with a subclass of the vanilla one that skips hidden items. A mod that registers its own item renderer wins or loses by load order; the label does not depend on it. Minecraft 1.21.2 added render states to the vanilla renderer; the subclass only inherits them, and the client gametest runs on 1.21.8 to check that the 1.21 build still works there. On Minecraft 26.x one jar covers 26.1 - 26.3 while 26.3 added the partial tick to `shouldRender`: the 26.x subclass overrides both forms, the older one calls the vanilla method through a method handle, and the compatibility check lists the resulting differences as reviewed (`.github/scripts/linkcheck-allowed-26.txt`).
- **Server resync.** The client counts down on its own ticks; the server resends the clocks of items within 32 blocks of each player with the mod every 5 seconds, so lag on either side does not add up.
- **HUD API drift.** Fabric 1.21 – 1.21.5 has only `HudRenderCallback`, 1.21.6+ has `HudElementRegistry` (chosen at runtime; the gametest covers the second), 26.x extracts render state, Forge and NeoForge have their own overlay events. Each is a few lines per band; the 26.x link check catches renames.
- **The client-only death timer** cannot see that unloaded chunks stop the items' clock, and counts items that merged with others or went through a portal as gone; the `≈` mark and the README say so. The server half follows the items by UUID across chunk unloads.

---

## Русский

### Модули

| Модуль | Что это |
|---|---|
| `common/` | Чистая Java 17 без Minecraft: протокол (`HelloMessage`, `ClockMessage`), отсчёт на клиенте (`ClientClocks`), кому какое обновление (`ClockSender`), ритм мигания (`Blink`), текст времени (`TimeText`), тексты сервера на 13 языках. Покрыт юнит-тестами. |
| `common-j8/` | Те же исходники в байткоде Java 8 через Jabel, для будущих полос 1.16.5 – 1.17 и плагина. |
| `shared/mc/` | Код Minecraft, одинаковый для всех загрузчиков: `ServerClocks` (следит за предметами, которые видит каждый игрок, и копит обновления), `StatusCommand`, `ClientTicker`. |
| `shared/fabric/` | Точки входа и события Fabric. |
| `versions/<загрузчик>/<полоса>/` | Проект Gradle на каждый загрузчик и диапазон Minecraft: Fabric 1.21 – 1.21.11, Fabric 26.1 – 26.3, Forge 1.20.1 (+ NeoForge 1.20.1), NeoForge 1.21 – 1.21.1. |
| `universal/`, `packaging/` | Диспетчеры и `MergeJars`: полосы собираются в **два jar** (Fabric/Quilt, Forge/NeoForge), которые при запуске выбирают нужную полосу. |
| `shared/gametest/` | Клиентский gametest Fabric: бросает предметы в настоящем мире, проверяет полученное время, делает скриншоты. |
| `plugin/` (позже) | Плагин для Paper / Folia / Spigot на том же протоколе. |

### Этапы

1. **Каркас** (этот PR): модули выше, протокол и серверная часть на всех четырёх приоритетных целях, `/despawnindicator`, CI (сборка, самые старые загрузчики, проверка линковки 26.x, запуск серверов на 12 парах загрузчик/версия, клиентский gametest под xvfb), workflow релиза и площадок, README / CONTRIBUTING / CHANGELOG.
2. **MVP → 0.1.0**: мигание последние 20 с (пропуск кадров), подпись со временем, когда смотришь на предмет или крадёшься рядом, оценка без серверного мода (`≈`), точное время в одиночной игре и с модом на сервере; скриншоты из gametest; релиз на Modrinth и CurseForge.
3. **HUD вещей после смерти** (сделано): после смерти таймер слева вверху со временем до исчезновения первой из ваших вещей, стрелкой, расстоянием, координатами и измерением; жёлтый в последнюю минуту, мигает красным последние 15 с, пропадает, когда вещи подобраны или исчезли. Сервер с модом следит за вещами (`DeathLoot`, протокол 2) и шлёт обновление, только когда счёт клиента разошёлся бы; без него клиент угадывает (`≈`). Настройки клиента (время предупреждения, стиль, когда показывать подписи, HUD после смерти, звук) и опция сервера (часы для всех предметов или только своих) лежат в файлах `.properties` в `config/`. Gametest: умирает, возрождается в 30 блоках, проверяет таймер и что он пропадает, когда вещи подобраны.
4. **Плагин** для Paper 1.16.5+, Folia, Spigot: те же каналы hello/clocks; читает `alt-item-despawn-rate` из Paper (по типам предметов, через рефлексию, потому что API конфига меняется между версиями) и `item-despawn-rate` из Spigot.
5. **Плавное исчезновение, безопасное для шейдеров**, как опция рядом с миганием; проверка с Iris/Oculus.
6. **Больше версий**: 1.16.5, 1.17.1, 1.18.2, 1.19.2/1.19.4, 1.20.4 – 1.20.6 и новые 26.x по мере выхода.
7. **Страницы на площадках**: галерея из скриншотов gametest, страница CurseForge.

### Цели MVP (0.1.0)

- Игрок видит, какие предметы вот-вот исчезнут и сколько осталось любому предмету.
- Работает **только на клиенте** на любом сервере; точно — с модом или плагином на сервере; ванильные клиенты на сервере с модом ничего не замечают.
- Нагрузка на сервер мала: обновления только для видимых игроку предметов, только игрокам с модом, раз в тик пачкой, несколько байт на предмет.
- Правильно выглядит с шейдерами.

### Что взято из voice-physics и MultiClicker

- **Полосы и универсальные jar** из voice-physics: проекты Gradle по диапазонам, `MergeJars`, `BandAdapter` для Fabric и диспетчеры Forge / NeoForge, Jabel для байткода Java 8.
- **CI** из voice-physics: Build & Verify с самыми старыми загрузчиками, проверка линковки 26.x, In-game tests (запуск серверов + клиентский gametest под xvfb с Mesa), повтор при сбое сети, релиз из `CHANGELOG.md`, публикация на Modrinth / CurseForge с пропуском площадки, пока нет id или токена, workflow страницы Modrinth и описания репозитория.
- **Правила документации** из обоих: README в двух файлах с одинаковыми разделами, CHANGELOG и CONTRIBUTING сначала по-английски, потом по-русски, двуязычные шаблоны issue и PR.
- **Из MultiClicker:** небольшой мод с клиентом и необязательной серверной частью и команда состояния, по которой владелец сервера видит, у кого стоит мод.

### Что сделано иначе и почему

- **Сырые байты вместо кодеков пакетов загрузчика.** Мод на четырёх загрузчиках и будущий плагин должны писать одни и те же байты; простой формат на VarInt в `common` — единственное место, где они определены, а `ClockMessageTest` их фиксирует. `hello` с номером протокола позволяет серверу говорить только с клиентами, которые его понимают.
- **Мигание пропуском кадров, а не прозрачностью.** Полупрозрачные предметы чаще всего ломаются шейдерпаками; если в некоторых кадрах предмет просто не рисуется, это выглядит одинаково с шейдерами и без. Плавное исчезновение можно добавить позже опцией.
- **Два jar с первого релиза**, а не по файлу на версию: игрокам проще выбрать, и одна загрузка на семейство загрузчиков.
- **13 языков сразу** и тест, который держит ключи и плейсхолдеры в согласии: текстов мало, и они короткие.
- **Оценка на клиенте помечена `≈`.** Конкуренты либо ничего не показывают без серверного мода, либо выдают догадку за точное время.
- **Настройки в файле `.properties`, экрана настроек пока нет.** Файл перечитывается при изменении, так что правки действуют сразу в игре; экран требует GUI под каждый загрузчик и версию, а Mod Menu есть только на Fabric. Догадка таймера после смерти тоже помечена `≈`.
- **Стиль `fade` плавно гасит только таймер после смерти;** предметы мигают, пока нет безопасного для шейдеров исчезновения (этап 5).
- **Экраны рисуются через свой `Canvas`.** `GuiGraphics` менялся между 1.20.1, 1.21 и 26.x (в 26.x строится render state, а не рисуется); в каждой полосе свой небольшой `GuiCanvas`, а `LootOverlay` в `shared/mc` везде один.

### Конкуренты (Modrinth, CurseForge)

ClearDespawn, ClearDespawn Reborn, Despawn Notifier, FadingLoot, Item Despawn Timer, DelTime, DespawnCountdown; плагины itemdespawntimer и bItemDespawn. Ни у одного нет общего протокола мода и плагина, учёта `alt-item-despawn-rate` из Paper, мигания, безопасного для шейдеров, и HUD вещей после смерти со стрелкой; это и есть ниша мода.

### Риски

- **Изменения API в 1.21.x.** Один jar Fabric работает на 1.21 – 1.21.11; методов, чья сигнатура менялась между выпусками (`Entity.level()`, `Entity.position()`, `GameProfile.getName()`), нужно избегать. Запуск серверов проверяет 1.21, 1.21.1, 1.21.5, 1.21.8, 1.21.11.
- **В Forge 1.20.1 сервер не знает язык игрока**, поэтому `/despawnindicator` там отвечает по-английски.
- **В списке версий для публикации есть Forge 1.21** у файла Forge/NeoForge: площадки принимают один список версий игры на файл, а NeoForge нужны 1.21 – 1.21.1. Jar не заявляет поддержку Forge 1.21 и там не загружается; это сказано в README, позже можно разделить на две загрузки.
- **Оценка только на клиенте** неверна для предметов, которые уже лежали; об этом говорят значок `≈` и README.
- **Мигание заменяет отрисовщик предметов** подклассом ванильного, который пропускает скрытые предметы. Мод, регистрирующий свой отрисовщик предметов, выигрывает или проигрывает по порядку загрузки; подпись от этого не зависит. В Minecraft 1.21.2 у ванильного отрисовщика появились render state; подкласс их только наследует, а клиентский gametest идёт на 1.21.8 и проверяет, что сборка 1.21 там работает.
- **Повторная синхронизация.** Клиент отсчитывает время по своим тикам; сервер каждые 5 секунд заново отправляет часы предметов в 32 блоках от каждого игрока с модом, поэтому лаги с обеих сторон не накапливаются.
- **Изменения API HUD.** В Fabric 1.21 – 1.21.5 есть только `HudRenderCallback`, в 1.21.6+ — `HudElementRegistry` (выбор при запуске; gametest проверяет второй), в 26.x извлекается render state, у Forge и NeoForge свои события оверлеев. На полосу это несколько строк; проверка линковки 26.x ловит переименования.
- **Таймер после смерти без серверного мода** не видит, что в выгруженных чанках часы предметов стоят, и считает пропавшими предметы, слившиеся с другими или ушедшие в портал; об этом говорят значок `≈` и README. Серверная часть находит предметы по UUID и после выгрузки чанка.
