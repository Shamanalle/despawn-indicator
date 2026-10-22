# Plan / План

**[English](#english)** · **[Русский](#русский)**

---

## English

### Modules

| Module | What it is |
|---|---|
| `common/` | Plain Java 17, no Minecraft: the wire protocol (`HelloMessage`, `ClockMessage`), the client countdown (`ClientClocks`), who gets which update (`ClockSender`), blink timing (`Blink`), time text (`TimeText`), server texts in 13 languages. Unit-tested. |
| `common-j8/` | The same sources compiled to Java 8 bytecode with Jabel, for the 1.16.5 and 1.17.1 bands and the universal jars. |
| `shared/mc/` | Minecraft code that is the same on every loader: `ServerClocks` (tracks the items each player can see and queues updates), `StatusCommand`, `ClientTicker`. |
| `shared/fabric/` | Fabric entrypoints and events. |
| `versions/<loader>/<band>/` | One Gradle project per loader and Minecraft range: Fabric 1.16.5, 1.17.1, 1.18.2, 1.19 – 1.19.3, 1.19.4, 1.20 – 1.20.1, 1.20.2 – 1.20.4, 1.20.5 – 1.20.6, 1.21 – 1.21.1, 1.21.2 – 1.21.8, 1.21.9 – 1.21.11 and 26.1 – 26.3, Forge 1.20.1 (+ NeoForge 1.20.1), NeoForge 1.21 – 1.21.1, 1.21.2 – 1.21.5, 1.21.6 – 1.21.8, 1.21.9 – 1.21.10, 1.21.11 and 26.1 – 26.3. |
| `universal/`, `packaging/` | Dispatchers and `MergeJars`: the bands are merged into **two jars** (Fabric/Quilt, Forge/NeoForge) that pick the right band at startup. |
| `shared/gametest/` | Fabric client gametest: drops items in a real world, checks the times the client got, takes screenshots. |
| `plugin/` | Paper / Folia / Spigot plugin speaking the same protocol: `common` compiled to Java 8 with Jabel, built against the Paper 1.16.5 API; the Folia scheduler is a separate source set loaded only on Folia. |

### Phases

1. **Skeleton** (this PR): the modules above, the protocol and the server half on all four priority targets, `/despawnindicator`, CI (build, oldest loaders, 26.x link check, server smoke on 12 loader/version pairs, client gametest under xvfb), release and store workflows, README / CONTRIBUTING / CHANGELOG.
2. **MVP → 0.1.0**: blinking in the last 20 s (frame skip), a time label when you look at an item or sneak near it, the client-only estimate (`≈`), exact times in singleplayer and with the server mod; gametest screenshots of both; release on Modrinth and CurseForge.
3. **Death loot HUD** (done): after you die, a timer in the top left with the time until the first of your drops despawns, an arrow, the distance, the coordinates and the dimension; yellow in the last minute, blinking red in the last 15 s, gone once the items are picked up or despawn. A server with the mod tracks the drops (`DeathLoot`, protocol 2) and sends updates only when the client's count would drift; without it the client guesses (`≈`). Client settings (warning time, style, when labels show, death HUD, sound) and a server option (clocks for all items or only your own) live in `.properties` files under `config/`. Gametest: dies, respawns 30 blocks away, checks the timer and that it goes when the items are picked up.
4. **Plugin** (done) for Paper 1.16.5+, Folia, Spigot: the same hello/clocks channels and death loot timer (protocol 2), `/despawnindicator`, the `clocks=all|own` option. Each item's age and despawn age are read from the server's item entity through reflection, which covers Paper's per-type `alt-item-despawn-rate`; Spigot's `item-despawn-rate` is the fallback. CI starts Paper 1.16.5, 1.17.1, 1.20.4, 1.21.11, 26.1.2 and Folia 1.21.11 with the plugin, joins with a bot that speaks the mod's protocol, throws a stack, dies and checks the clocks, the death loot timer and `/despawnindicator`.
5. **Shader-safe fade** (done): `style=fade` makes an item shrink and grow back toward the height it floats at instead of blinking, in the same rhythm, never below 30 % of its size; only the pose is scaled, so shader packs draw it like any item. Every renderer (Fabric 1.21, 1.21.2, 1.21.9, 26.x; Forge 1.20.1; NeoForge 1.21) does it, and the client gametest switches the style in the config file and checks the sizes and a screenshot. To draw through the render states of 1.21.2+ and 1.21.9+, the Fabric 1.21 band is split into three (1.21 – 1.21.1, 1.21.2 – 1.21.8, 1.21.9 – 1.21.11), and the link check runs for each band on every release of its range.
6. **More versions**: Fabric 1.20 – 1.20.6 (done: three bands in the universal Fabric jar; CI starts servers on 1.20, 1.20.1, 1.20.4 and 1.20.6 and link-checks each band on every release of its range); Fabric 1.19 – 1.19.4 (done: two bands, 1.19 – 1.19.3 and 1.19.4, split where `ItemEntity.getOwner()` changed; CI starts servers on 1.19, 1.19.2, 1.19.3 and 1.19.4); Fabric 1.18.2 (done: a band of its own with the 1.19.x client code, the v1 command API of Fabric API, CI starts a 1.18.2 server); Fabric 1.17.1 (done: the 1.18.2 sources built for Java 16 against `common-j8`, the universal jar became `+mc1.17-26.x`; CI starts a 1.17.1 server and link-checks the band); Fabric 1.16.5 (done: a band for Java 8 compiled with Jabel, with its own item renderer, log and `Compat`, since 1.16.5 marks no item unloaded with its chunk; the universal jar is now `+mc1.16-26.x`; CI starts a 1.16.5 server on Java 8 and link-checks the band); NeoForge 1.21.2 – 26.3 (done: five more bands, split where NeoForge's HUD layers and networking or Minecraft's item renderer changed, all in the one Forge/NeoForge jar, now `+mc1.20.1-26.x`; the 26.x band is compiled to Java 21 class files, because Forge 1.20.1 reads every class of that jar; CI starts servers on the oldest and the newest NeoForge of each band and link-checks each band on the newest NeoForge of every Minecraft version of its range and on the oldest it allows); next more Forge versions and the rest of 26.x as they come out.
7. **Store pages** (done): four screenshots from the client gametest on 26.3 (time left when sneaking and when looking, the death loot timer, `/despawnindicator`) in `docs/images` and `docs/gallery.json`, which the *Modrinth page* workflow uploads; the first one is also on the page itself and in the README, so the text pasted into CurseForge carries it; the CurseForge steps (description, *Images*) are in `docs/store-description.md`.

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
- **Blink by skipping frames, not by transparency.** Translucent item rendering is what shader packs break most often; not drawing the item on some frames looks the same with or without shaders.
- **Two jars from the first release**, not one per version: fewer files for players to choose from and one upload per loader family.
- **13 languages from the start**, with a test that keeps keys and placeholders in step, because the texts are few and short.
- **Client-side estimate is marked `≈`.** Competitors either show nothing without a server mod or show a guess as if it were exact.
- **Settings in a `.properties` file, no settings screen yet.** The file is read again when it changes, so edits apply in game; a screen needs a GUI per loader and version, and Mod Menu only covers Fabric. The death timer guess is also marked `≈`.
- **The fade shrinks the item instead of making it transparent.** Translucency is what shader packs break (see blinking above); a smaller item is drawn the usual way. It never vanishes, so a fading item stays easy to find. The death loot timer is drawn on the HUD, which shaders do not touch, and still fades by transparency.
- **Three Fabric 1.21 bands instead of one.** The renderer has to change the item's size, which goes through the render state that 1.21.2 introduced and 1.21.9 changed again; one subclass cannot override the draw method of all three. Each band is compiled against the last release of its range, and the link check (`.github/workflows/compat.yml`, formerly 26.x only) now runs every band against every release of its range; it caught two calls whose signature changed within 1.21.2 – 1.21.8 (`SoundManager.play`, `ServerLevel.getEntity(UUID)`), now replaced by stable ones. The players still get one Fabric jar.
- **The plugin is on Modrinth only.** CurseForge keeps Bukkit plugins in a separate kind of project (*Bukkit Plugins*) with its own page and id; one more project to keep in step is not worth it while Modrinth lists Paper, Folia and Spigot next to the mod.
- **The plugin reads the item entity instead of the config.** Paper's `alt-item-despawn-rate` is applied to each item when it spawns and the config API changed several times; the value on the item is what the server will actually use, and it also covers Forge-based hybrid servers.
- **Screens are drawn through our own `Canvas`.** `GuiGraphics` changed between 1.20.1, 1.21 and 26.x (26.x builds render state instead of drawing); each band has a small `GuiCanvas`, and `LootOverlay` in `shared/mc` stays the same everywhere.
- **Each Fabric band owns its networking.** Fabric API sends packets by channel name up to 1.20.4 and as typed payloads from 1.20.5, so `ModNetworking` (server: register, send, the player's language) and `client/ClientNetworking` (register, hello) live in the band and the shared Fabric code only calls them. The bytes are the same everywhere. Fabric 1.20.x has three bands instead of one because of that and because the server learns the player's language only from 1.20.2; the HUD and the item renderer are the 1.21 ones. Fabric API for 1.20.x has no client gametest, so CI checks these bands with servers and the link check only.

### Competitors (Modrinth, CurseForge)

ClearDespawn, ClearDespawn Reborn, Despawn Notifier, FadingLoot, Item Despawn Timer, DelTime, DespawnCountdown; plugins itemdespawntimer and bItemDespawn. None of them shares one protocol between the mod and a plugin, follows Paper's `alt-item-despawn-rate`, blinks safely under shaders and has a death loot HUD with an arrow; that set is this mod's place.

### Risks

- **1.21.x API drift.** Each Fabric band runs on several releases; methods whose signature changed between them (`Entity.level()`, `Entity.position()`, `GameProfile.getName()`, `SoundManager.play`) must be avoided. The link check runs every band on every release of its range; server smoke covers 1.21, 1.21.1, 1.21.5, 1.21.8, 1.21.11.
- **Forge 1.20.1 and Fabric 1.16.5 – 1.20.1 have no player language on the server**, so `/despawnindicator` answers in English there. The client could send its language in `hello`, but that is a protocol change for every loader and the plugin.
- **The publish list includes Forge 1.21 and up** for the Forge/NeoForge file, because the stores take one list of game versions per file and NeoForge needs 1.21 – 26.3. The jar declares no Forge 1.21+ support and does not load there; this is said in the README. Two uploads of the same jar are not possible: Modrinth refuses a file it already has.
- **The client-only estimate** is wrong for items that were already lying there; the `≈` mark and the README say so.
- **Blinking replaces the item renderer** with a subclass of the vanilla one that skips hidden items. A mod that registers its own item renderer wins or loses by load order; the label does not depend on it. Each band's subclass draws through that band's render state; the client gametest runs the 1.21.2, 1.21.9 and 26.x bands on their newest release (1.21 – 1.21.1 have no client gametest in Fabric API). On Minecraft 26.x one jar covers 26.1 - 26.3 while 26.3 added the partial tick to `shouldRender`: the 26.x subclass overrides both forms, the older one calls the vanilla method through a method handle, and the compatibility check lists the resulting differences as reviewed (`.github/scripts/linkcheck-allowed-26.txt`).
- **Server resync.** The client counts down on its own ticks; the server resends the clocks of items within 32 blocks of each player with the mod every 5 seconds, so lag on either side does not add up.
- **HUD API drift.** Fabric 1.21 – 1.21.5 has only `HudRenderCallback`, 1.21.6+ has `HudElementRegistry` (chosen at runtime; the gametest covers the second), 26.x extracts render state, Forge and NeoForge have their own overlay events. Each is a few lines per band; the link check catches renames. Inside a NeoForge band, what its oldest version lacks (`ClientPacketDistributor` before NeoForge 21.7, `Gui.hud` before Minecraft 26.2) is called only where it exists, with the older way found by method handle; each such difference is listed in `linkcheck-allowed-neoforge-<band>.txt`.
- **The client-only death timer** cannot see that unloaded chunks stop the items' clock, and counts items that merged with others or went through a portal as gone; the `≈` mark and the README say so. The server half follows the items by UUID across chunk unloads.
- **Plugin internals by reflection.** The item's age and despawn age live in server classes whose names changed (obfuscated 1.17 – 1.20.4, Mojang names after); a failed lookup falls back to `getTicksLived()` and `item-despawn-rate` with one warning. The CI matrix covers the oldest, an obfuscated, the newest 1.x and a 26.x Paper.
- **Spigot is not tested in CI**: it needs BuildTools to build each version. It shares the Bukkit API with Paper; the plugin uses no Paper-only calls without a fallback.
- **Folia.** Each item is read on its own region's thread; a death loot item in a region that is not loaded counts as paused. `/reload` loses which protocol each client speaks, so players online during a reload get item clocks without the death loot timer until they rejoin.
- **Shaders are not tested in CI.** Iris and Oculus need a GPU the runners lack; the fade and the blink only change what vanilla draws (the pose, or whether the item is drawn), which is the path shader packs already handle.

---

## Русский

### Модули

| Модуль | Что это |
|---|---|
| `common/` | Чистая Java 17 без Minecraft: протокол (`HelloMessage`, `ClockMessage`), отсчёт на клиенте (`ClientClocks`), кому какое обновление (`ClockSender`), ритм мигания (`Blink`), текст времени (`TimeText`), тексты сервера на 13 языках. Покрыт юнит-тестами. |
| `common-j8/` | Те же исходники в байткоде Java 8 через Jabel, для полос 1.16.5 и 1.17.1 и общих jar. |
| `shared/mc/` | Код Minecraft, одинаковый для всех загрузчиков: `ServerClocks` (следит за предметами, которые видит каждый игрок, и копит обновления), `StatusCommand`, `ClientTicker`. |
| `shared/fabric/` | Точки входа и события Fabric. |
| `versions/<загрузчик>/<полоса>/` | Проект Gradle на каждый загрузчик и диапазон Minecraft: Fabric 1.16.5, 1.17.1, 1.18.2, 1.19 – 1.19.3, 1.19.4, 1.20 – 1.20.1, 1.20.2 – 1.20.4, 1.20.5 – 1.20.6, 1.21 – 1.21.1, 1.21.2 – 1.21.8, 1.21.9 – 1.21.11 и 26.1 – 26.3, Forge 1.20.1 (+ NeoForge 1.20.1), NeoForge 1.21 – 1.21.1, 1.21.2 – 1.21.5, 1.21.6 – 1.21.8, 1.21.9 – 1.21.10, 1.21.11 и 26.1 – 26.3. |
| `universal/`, `packaging/` | Диспетчеры и `MergeJars`: полосы собираются в **два jar** (Fabric/Quilt, Forge/NeoForge), которые при запуске выбирают нужную полосу. |
| `shared/gametest/` | Клиентский gametest Fabric: бросает предметы в настоящем мире, проверяет полученное время, делает скриншоты. |
| `plugin/` | Плагин для Paper / Folia / Spigot на том же протоколе: `common` в байткоде Java 8 через Jabel, сборка против API Paper 1.16.5; планировщик Folia — отдельный набор исходников, загружается только на Folia. |

### Этапы

1. **Каркас** (этот PR): модули выше, протокол и серверная часть на всех четырёх приоритетных целях, `/despawnindicator`, CI (сборка, самые старые загрузчики, проверка линковки 26.x, запуск серверов на 12 парах загрузчик/версия, клиентский gametest под xvfb), workflow релиза и площадок, README / CONTRIBUTING / CHANGELOG.
2. **MVP → 0.1.0**: мигание последние 20 с (пропуск кадров), подпись со временем, когда смотришь на предмет или крадёшься рядом, оценка без серверного мода (`≈`), точное время в одиночной игре и с модом на сервере; скриншоты из gametest; релиз на Modrinth и CurseForge.
3. **HUD вещей после смерти** (сделано): после смерти таймер слева вверху со временем до исчезновения первой из ваших вещей, стрелкой, расстоянием, координатами и измерением; жёлтый в последнюю минуту, мигает красным последние 15 с, пропадает, когда вещи подобраны или исчезли. Сервер с модом следит за вещами (`DeathLoot`, протокол 2) и шлёт обновление, только когда счёт клиента разошёлся бы; без него клиент угадывает (`≈`). Настройки клиента (время предупреждения, стиль, когда показывать подписи, HUD после смерти, звук) и опция сервера (часы для всех предметов или только своих) лежат в файлах `.properties` в `config/`. Gametest: умирает, возрождается в 30 блоках, проверяет таймер и что он пропадает, когда вещи подобраны.
4. **Плагин** (сделано) для Paper 1.16.5+, Folia, Spigot: те же каналы hello/clocks и таймер вещей после смерти (протокол 2), `/despawnindicator`, опция `clocks=all|own`. Возраст предмета и возраст исчезновения читаются из сущности предмета на сервере через рефлексию, что учитывает `alt-item-despawn-rate` из Paper по типам предметов; запасной вариант — `item-despawn-rate` из Spigot. CI запускает Paper 1.16.5, 1.17.1, 1.20.4, 1.21.11, 26.1.2 и Folia 1.21.11 с плагином, заходит ботом, который говорит на протоколе мода, бросает стак, умирает и проверяет часы, таймер вещей после смерти и `/despawnindicator`.
5. **Затухание, безопасное для шейдеров** (сделано): при `style=fade` предмет вместо мигания сжимается к высоте, на которой парит, и растёт обратно в том же ритме, не меньше 30 % размера; меняется только матрица, поэтому шейдерпаки рисуют его как любой предмет. Это умеет каждый отрисовщик (Fabric 1.21, 1.21.2, 1.21.9, 26.x; Forge 1.20.1; NeoForge 1.21), а клиентский gametest переключает стиль в файле настроек и проверяет размеры и скриншот. Чтобы рисовать через render state 1.21.2+ и 1.21.9+, полоса Fabric 1.21 разделена на три (1.21 – 1.21.1, 1.21.2 – 1.21.8, 1.21.9 – 1.21.11), а проверка линковки идёт для каждой полосы на каждом выпуске её диапазона.
6. **Больше версий**: Fabric 1.20 – 1.20.6 (сделано: три полосы в общем jar Fabric; CI запускает серверы 1.20, 1.20.1, 1.20.4 и 1.20.6 и проверяет линковку каждой полосы на каждом выпуске её диапазона); Fabric 1.19 – 1.19.4 (сделано: две полосы, 1.19 – 1.19.3 и 1.19.4, разделённые там, где изменился `ItemEntity.getOwner()`; CI запускает серверы 1.19, 1.19.2, 1.19.3 и 1.19.4); Fabric 1.18.2 (сделано: своя полоса с клиентским кодом 1.19.x и командным API v1 из Fabric API, CI запускает сервер 1.18.2); Fabric 1.17.1 (сделано: исходники 1.18.2, собранные для Java 16 с `common-j8`, общий jar стал `+mc1.17-26.x`; CI запускает сервер 1.17.1 и проверяет линковку полосы); Fabric 1.16.5 (сделано: полоса для Java 8, собранная через Jabel, со своей отрисовкой предметов, логом и `Compat`, потому что 1.16.5 не помечает предметы, выгруженные вместе с чанком; общий jar теперь `+mc1.16-26.x`; CI запускает сервер 1.16.5 на Java 8 и проверяет линковку полосы); NeoForge 1.21.2 – 26.3 (сделано: ещё пять полос, разделённых там, где менялись слои HUD и сеть NeoForge или отрисовка предметов в Minecraft, все в одном jar Forge/NeoForge, теперь `+mc1.20.1-26.x`; полоса 26.x собирается в классы Java 21, потому что Forge 1.20.1 читает каждый класс этого jar; CI запускает серверы на самом старом и самом новом NeoForge каждой полосы и проверяет линковку каждой полосы на новейшем NeoForge каждой версии Minecraft её диапазона и на самом старом допустимом); дальше другие версии Forge и новые 26.x по мере выхода.
7. **Страницы на площадках** (сделано): четыре скриншота из клиентского gametest на 26.3 (время при подкрадывании и при взгляде, таймер вещей после смерти, `/despawnindicator`) в `docs/images` и `docs/gallery.json`, их выкладывает workflow *Modrinth page*; первый есть и на самой странице, и в README, так что текст, вставленный в CurseForge, несёт его с собой; шаги для CurseForge (описание, *Images*) — в `docs/store-description.md`.

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
- **Мигание пропуском кадров, а не прозрачностью.** Полупрозрачные предметы чаще всего ломаются шейдерпаками; если в некоторых кадрах предмет просто не рисуется, это выглядит одинаково с шейдерами и без.
- **Два jar с первого релиза**, а не по файлу на версию: игрокам проще выбрать, и одна загрузка на семейство загрузчиков.
- **13 языков сразу** и тест, который держит ключи и плейсхолдеры в согласии: текстов мало, и они короткие.
- **Оценка на клиенте помечена `≈`.** Конкуренты либо ничего не показывают без серверного мода, либо выдают догадку за точное время.
- **Настройки в файле `.properties`, экрана настроек пока нет.** Файл перечитывается при изменении, так что правки действуют сразу в игре; экран требует GUI под каждый загрузчик и версию, а Mod Menu есть только на Fabric. Догадка таймера после смерти тоже помечена `≈`.
- **Затухание уменьшает предмет, а не делает его прозрачным.** Прозрачность ломают шейдерпаки (см. мигание выше); уменьшенный предмет рисуется обычным путём. Он не пропадает совсем, поэтому затухающий предмет легко найти. Таймер вещей после смерти рисуется в HUD, который шейдеры не трогают, и по-прежнему гаснет прозрачностью.
- **Три полосы Fabric 1.21 вместо одной.** Отрисовщик должен менять размер предмета, а это идёт через render state, появившийся в 1.21.2 и снова изменённый в 1.21.9; один подкласс не может переопределить метод отрисовки всех трёх. Каждая полоса собирается против последнего выпуска своего диапазона, а проверка линковки (`.github/workflows/compat.yml`, раньше только для 26.x) теперь проверяет каждую полосу на каждом выпуске её диапазона; она нашла два вызова, чья сигнатура менялась внутри 1.21.2 – 1.21.8 (`SoundManager.play`, `ServerLevel.getEntity(UUID)`), теперь заменённые стабильными. У игроков по-прежнему один jar Fabric.
- **Плагин только на Modrinth.** CurseForge держит плагины Bukkit в отдельном виде проектов (*Bukkit Plugins*) со своей страницей и id; ещё один проект, который надо поддерживать в согласии, того не стоит, пока Modrinth показывает Paper, Folia и Spigot рядом с модом.
- **Плагин читает сущность предмета, а не конфиг.** `alt-item-despawn-rate` из Paper применяется к каждому предмету при появлении, а API конфига несколько раз менялся; значение у предмета — то, что сервер действительно использует, и оно же работает на гибридных серверах с Forge.
- **Экраны рисуются через свой `Canvas`.** `GuiGraphics` менялся между 1.20.1, 1.21 и 26.x (в 26.x строится render state, а не рисуется); в каждой полосе свой небольшой `GuiCanvas`, а `LootOverlay` в `shared/mc` везде один.
- **Сеть Fabric — своя в каждой полосе.** Fabric API до 1.20.4 шлёт пакеты по имени канала, а с 1.20.5 — типизированными payload, поэтому `ModNetworking` (сервер: регистрация, отправка, язык игрока) и `client/ClientNetworking` (регистрация, hello) лежат в полосе, а общий код Fabric их только вызывает. Байты везде одни и те же. Из-за этого, а ещё потому что сервер узнаёт язык игрока только с 1.20.2, у Fabric 1.20.x три полосы, а не одна; HUD и отрисовщик предметов взяты из 1.21. У Fabric API для 1.20.x нет клиентского gametest, поэтому CI проверяет эти полосы только серверами и проверкой линковки.

### Конкуренты (Modrinth, CurseForge)

ClearDespawn, ClearDespawn Reborn, Despawn Notifier, FadingLoot, Item Despawn Timer, DelTime, DespawnCountdown; плагины itemdespawntimer и bItemDespawn. Ни у одного нет общего протокола мода и плагина, учёта `alt-item-despawn-rate` из Paper, мигания, безопасного для шейдеров, и HUD вещей после смерти со стрелкой; это и есть ниша мода.

### Риски

- **Изменения API в 1.21.x.** Каждая полоса Fabric работает на нескольких выпусках; методов, чья сигнатура между ними менялась (`Entity.level()`, `Entity.position()`, `GameProfile.getName()`, `SoundManager.play`), нужно избегать. Проверка линковки проходит каждую полосу на каждом выпуске её диапазона; запуск серверов проверяет 1.21, 1.21.1, 1.21.5, 1.21.8, 1.21.11.
- **В Forge 1.20.1 и Fabric 1.16.5 – 1.20.1 сервер не знает язык игрока**, поэтому `/despawnindicator` там отвечает по-английски. Клиент мог бы присылать язык в `hello`, но это изменение протокола для всех загрузчиков и плагина.
- **В списке версий для публикации есть Forge 1.21 и новее** у файла Forge/NeoForge: площадки принимают один список версий игры на файл, а NeoForge нужны 1.21 – 26.3. Jar не заявляет поддержку Forge 1.21+ и там не загружается; это сказано в README. Загрузить тот же jar дважды нельзя: Modrinth отказывает в файле, который у него уже есть.
- **Оценка только на клиенте** неверна для предметов, которые уже лежали; об этом говорят значок `≈` и README.
- **Мигание заменяет отрисовщик предметов** подклассом ванильного, который пропускает скрытые предметы. Мод, регистрирующий свой отрисовщик предметов, выигрывает или проигрывает по порядку загрузки; подпись от этого не зависит. Подкласс каждой полосы рисует через render state этой полосы; клиентский gametest запускает полосы 1.21.2, 1.21.9 и 26.x на их новейшем выпуске (для 1.21 – 1.21.1 в Fabric API нет клиентского gametest). В Minecraft 26.x один jar покрывает 26.1 – 26.3, а в 26.3 у `shouldRender` появился partial tick: подкласс 26.x переопределяет обе формы, старая вызывает ванильный метод через method handle, а проверка линковки отмечает получившиеся различия как проверенные (`.github/scripts/linkcheck-allowed-26.txt`).
- **Повторная синхронизация.** Клиент отсчитывает время по своим тикам; сервер каждые 5 секунд заново отправляет часы предметов в 32 блоках от каждого игрока с модом, поэтому лаги с обеих сторон не накапливаются.
- **Изменения API HUD.** В Fabric 1.21 – 1.21.5 есть только `HudRenderCallback`, в 1.21.6+ — `HudElementRegistry` (выбор при запуске; gametest проверяет второй), в 26.x извлекается render state, у Forge и NeoForge свои события оверлеев. На полосу это несколько строк; проверка линковки ловит переименования. Внутри полосы NeoForge то, чего нет в её самой старой версии (`ClientPacketDistributor` до NeoForge 21.7, `Gui.hud` до Minecraft 26.2), вызывается только там, где оно есть, а старый путь находится через method handle; каждое такое различие записано в `linkcheck-allowed-neoforge-<полоса>.txt`.
- **Таймер после смерти без серверного мода** не видит, что в выгруженных чанках часы предметов стоят, и считает пропавшими предметы, слившиеся с другими или ушедшие в портал; об этом говорят значок `≈` и README. Серверная часть находит предметы по UUID и после выгрузки чанка.
- **Внутренности сервера в плагине через рефлексию.** Возраст предмета и возраст исчезновения лежат в классах сервера, чьи имена менялись (обфусцированы в 1.17 – 1.20.4, имена Mojang позже); если поиск не удался, плагин один раз предупреждает и берёт `getTicksLived()` и `item-despawn-rate`. Матрица CI покрывает самый старый, обфусцированный, самый новый 1.x и 26.x Paper.
- **Spigot не проверяется в CI**: каждую версию надо собирать через BuildTools. API Bukkit у него общий с Paper; вызовов только из Paper без запасного варианта плагин не делает.
- **Folia.** Каждый предмет читается в потоке своего региона; вещь после смерти в незагруженном регионе считается на паузе. `/reload` теряет, на каком протоколе говорит каждый клиент, поэтому игроки, бывшие онлайн во время перезагрузки, получают часы предметов без таймера вещей после смерти, пока не перезайдут.
- **Шейдеры не проверяются в CI.** Iris и Oculus нужен GPU, которого у раннеров нет; затухание и мигание меняют только то, что рисует ваниль (матрицу или то, рисуется ли предмет), а этот путь шейдерпаки уже обрабатывают.
