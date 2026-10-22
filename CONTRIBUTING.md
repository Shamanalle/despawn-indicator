# Contributing / Участие в разработке

**[English](#english)** · **[Русский](#русский)**

---

## English

### Setup

- **JDK 25**: it builds every module (Forge 1.20.1 and the 1.20 / 1.21 bands are compiled with `--release 17` / `21`).
- Git.

```bash
git clone https://github.com/Shamanalle/despawn-indicator.git
cd despawn-indicator
./gradlew :common:test             # fast: protocol, countdown, texts, translations
./gradlew build                    # every target, the two jars in build/libs/
./gradlew :fabric-1.21.2:runClient   # or :fabric-1.17, :fabric-1.18, :fabric-1.19, :fabric-1.19.4, :fabric-1.20, :fabric-1.20.2, :fabric-1.20.5, :fabric-1.21, :fabric-1.21.9, :fabric-26, :forge-1.20.1, :neoforge-1.21; runServer for the server side
```

### Where code lives

| Directory | Compiled into | Contains |
|---|---|---|
| `common/` | every jar (Java 17, no Minecraft classes) | client–server protocol (`ClockMessage`, `HelloMessage`, `Bytes`), the countdown (`ClientClocks`), who gets what (`ClockSender`), blinking (`Blink`), time text (`TimeText`), server texts (`ServerText`), **translations and icon** |
| `common-j8/` | `fabric-1.17`, the universal jars | the same sources as Java 8 bytecode (Jabel): the 1.17.1 band (Java 16) is built against them, and the universal jars store them once for every band |
| `shared/mc/` | every Fabric, Forge and NeoForge band | server side (`ServerClocks`: what to send and when; `StatusCommand`), client tick (`ClientTicker`) |
| `shared/fabric/` | the Fabric bands | entrypoints and Fabric API events, `Platform` / `ClientPlatform` for sending |
| `shared/fabric-command-v1/` | `fabric-1.17`, `fabric-1.18` | `ModCommands`: registers `/despawnindicator` with the command API v1 of Fabric API, the only one before 1.19 |
| `shared/fabric-command-v2/` | the Fabric bands from 1.19 on | `ModCommands`: the same with the command API v2 |
| `shared/mc-1.20/` | the Fabric bands from 1.20 on, `fabric-26`, `forge-1.20.1`, `neoforge-1.21` | `Compat`: calls whose names or types changed in 1.19.4 or 1.20 (texts, the player's level, command replies, an item's owner) |
| `shared/fabric-1.17/` | `fabric-1.17`, `fabric-1.18`, `fabric-1.19`, `fabric-1.19.4` | the channels, the item renderer and the HUD of 1.17.1 – 1.19.4 (`PoseStack` and `GuiComponent` instead of `GuiGraphics`) |
| `shared/mc-1.17/` | `fabric-1.17`, `fabric-1.18` | `Compat` and `ClientCompat`: texts and the note sound of 1.17.1 – 1.18.2 |
| `shared/mc-1.19/` | `fabric-1.19`, `fabric-1.19.4` | `ClientCompat`: the note sound of 1.19.x |
| `shared/gametest/` | `fabric-1.21.2`, `fabric-1.21.9`, `fabric-26` (client gametest) | drops items in a real world, checks their times and takes screenshots |
| `versions/fabric/<band>/` (`1.17`, `1.18`, `1.19`, `1.19.4`, `1.20`, `1.20.2`, `1.20.5`, `1.21`, `1.21.2`, `1.21.9`, `26`) | the Fabric / Quilt jar; the Gradle project is `:fabric-<band>` | the two channels and the player's language (`ModNetworking`, `client/ClientNetworking`: channels by name before 1.20.5, payloads from 1.20.5, other names in 26.x), the item renderer and the HUD hook (they changed in 1.21, 1.21.2 and 1.21.9), `fabric.mod.json` |
| `versions/forge/1.20.1/`, `versions/neoforge/1.21/` | the Forge / NeoForge jar; projects `:forge-1.20.1`, `:neoforge-1.21` | Forge and NeoForge hooks (`forge/`, `neoforge/`: entry, networking on the same channels and bytes, client), `Platform` with item lifespans, `mods.toml` / `neoforge.mods.toml`. Forge 1.20.1 runs with SRG names, so that band is remapped by `reobfJar` |
| `universal/` | both jars | the dispatchers that pick a band at startup (`BandAdapter` for Fabric, `ForgeDispatcher` / `NeoForgeDispatcher`) |
| `packaging/` | build only | `MergeJars`: merges the band jars into one, stores shared classes once and moves each band's own classes under `b/<band>/` |

Rules of thumb:
- Logic that does not need Minecraft goes into `common` and gets a unit test.
- Fabric 1.17.1 and 1.18.2 have a band each with the same sources: texts, the note sound and the command API changed in 1.19, the rest is the 1.19.x code. The 1.17.1 band is for Java 16, so it is built against `common-j8` instead of `common`. Fabric 1.19.x has two bands, split where `ItemEntity.getOwner()` changed: 1.19 – 1.19.3 (compiled against 1.19.2, because `PoseStack.translate(float, float, float)` only exists from 1.19.3) and 1.19.4. Their client code is in `shared/fabric-1.17/`, and each has its own `Compat`; the bands from 1.20 on share the one in `shared/mc-1.20/`. Fabric 1.20.x has three bands, split where the networking changed: 1.20 – 1.20.1 (the server does not learn the player's language), 1.20.2 – 1.20.4 (channels by name) and 1.20.5 – 1.20.6 (payloads, Java 21). Fabric 1.21.x has three, split where the way items are drawn changed: 1.21 – 1.21.1, 1.21.2 – 1.21.8 and 1.21.9 – 1.21.11. Each is compiled against the last version of its range (gradle.properties) and runs on all of them. Before using a new Minecraft or Fabric API method in `shared/`, check that its signature is the same on every release the bands cover; `.github/workflows/compat.yml` checks each band's jar on every release of its range. Examples that are **not** stable: `Entity.level()`, `Entity.position()`, `GameProfile.getName()`, `ServerLevel.getEntity(UUID)`, `SoundManager.play`, `ItemEntity.getOwner()`.
- Client and server exchange raw bytes on `despawnindicator:hello` (client → server) and `despawnindicator:clocks` (server → client). Every loader and the future plugin write exactly the same bytes; `ClockMessageTest` pins them down.
- The server sends only to players who said hello, at the end of the server tick, so a burst of drops becomes one packet.

### Translations

Language files are in `common/src/main/resources/assets/despawnindicator/lang/` (13 languages). Add every new key to **all** files. `LangConsistencyTest` fails if a key used in code is missing, if the languages have different keys, or if their placeholders differ.

### Documentation

The README is two files: `README.md` in English and `README.ru.md` in Russian. They have the same sections and are changed together. The rest of the documentation (CHANGELOG, this file, issue and pull request templates) is written in English first, followed by a complete Russian translation. The README is a reference for players and server owners, without internals.

GitHub release notes are generated from `CHANGELOG.md` by `.github/scripts/release-notes.sh`. When the changelog changes on `main`, the *Sync release notes* workflow updates the notes of already published releases.

The repository description and topics live in `.github/about.json`. The *Repository about* workflow validates the file in pull requests and applies it when it changes on `main`. Applying needs the `REPO_ADMIN_TOKEN` secret, because the built-in workflow token cannot change repository settings. It is set up once:
1. GitHub → *Settings → Developer settings → Personal access tokens → Fine-grained tokens → Generate new token*.
2. *Repository access*: *Only select repositories* → this repository. *Permissions → Repository permissions → Administration*: *Read and write*. Nothing else is needed.
3. In this repository: *Settings → Secrets and variables → Actions → New repository secret*, name `REPO_ADMIN_TOKEN`, value: the token.
4. Run *Actions → Repository about → Run workflow* once, or change `.github/about.json`. When the token expires, generate a new one and update the secret.

### In-game tests

*In-game tests* (`.github/workflows/ingame.yml`) runs on every pull request and before every release:
- **Servers:** `.github/scripts/server-smoke.sh` starts Fabric, Forge and NeoForge servers with the mod, runs `/despawnindicator` from the console and fails on a missing answer, bare text keys or an exception from the mod. Locally: `bash .github/scripts/server-smoke.sh fabric 1.21.1 build/libs/despawn-indicator-fabric-*+mc1.17-26.x.jar`.
- **Client:** `./gradlew :fabric-1.21.2:runClientGameTest` (or `:fabric-1.21.9`, `:fabric-26`) creates a world, drops a diamond that is about to despawn, an emerald that never despawns and a gold ingot, checks the times the client received, runs `/despawnindicator` and saves screenshots in `build/run/clientGameTest/screenshots/`. CI runs it under `xvfb-run` with Mesa's software OpenGL and Vulkan; the 1.21.x clients run on Java 21.
- Logs and screenshots are attached to each run as artifacts.
- **Network failures:** a download that fails on the way (a timeout, a reset connection, HTTP 429 / 5xx) is tried once more and leaves a warning on the run: the build and the client before the game starts (`.github/scripts/net-retry.sh`, the second time with Maven Central through Google's mirror), the server's installer and first start. A failing test, a compile error and a missing file (404) fail at once.

*Build & Verify* also compiles the Forge and NeoForge bands against the oldest loader they claim, and *Fabric compatibility* checks that each Fabric band's jar links against every release of its range (`LinkCheck.java`).

### Releases

1. Set `mod_version` in `gradle.properties` and add its section to `CHANGELOG.md` (English, then Russian).
2. Merge into `main` and wait for the green build. The release runs the in-game tests again and stops when they fail.
3. *Actions → Publish Release → Run workflow* on `main`, or push the tag `vX.Y.Z`. The workflow builds every jar, creates the tag and the release, and takes the notes from `CHANGELOG.md`.
4. The same run then uploads the files to Modrinth and CurseForge (see below). To upload an existing release again: *Actions → Publish to Modrinth & CurseForge → Run workflow* with its tag.

### Modrinth and CurseForge

The workflow *Publish to Modrinth & CurseForge* (`.github/workflows/publish.yml`) takes the two files of a GitHub release and uploads them to both stores. A store is skipped with a warning while its project id or token is missing. Setup, once:
1. Create the projects by hand: Modrinth → *Create a project* (type *Mod*); CurseForge → *Minecraft → Mods → Create project*. Name, summary, categories and a checklist are in `docs/store-description.md`. Both sites review new projects; the first upload can go into the project while it waits. The Modrinth page is `docs/modrinth.md` and is uploaded by the *Modrinth page* workflow.
2. Put the project ids into `.github/publish.json`: on Modrinth the *Project ID* from the project's menu, on CurseForge the *Project ID* number on the project page.
3. Tokens: Modrinth → *Settings → Personal access tokens*, scopes *Create versions*, *Read projects*, *Write versions* (lets *Sync release notes* update the changelog of Modrinth versions) and *Write projects* (lets *Modrinth page* upload the page and the gallery); CurseForge → *Account → API tokens*. Add them as repository secrets `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN` (*Settings → Secrets and variables → Actions*). Tokens never go into files, commits or chats.

### Pull requests

1. Branch off `main` (`feature/…`, `fix/…`).
2. Run `./gradlew :common:test` and `./gradlew build`.
3. Describe what changed and how you tested it in game (Minecraft and loader versions; client, server or both; with or without shaders).
4. Add an entry to `CHANGELOG.md` under an *Unreleased* section, in English and in Russian. The changelog becomes the release notes on Modrinth and CurseForge: short plain items about what players and server owners notice, no internals.

---

## Русский

### Подготовка

- **JDK 25**: собирает все модули (Forge 1.20.1 и полосы 1.20 / 1.21 компилируются с `--release 17` / `21`).
- Git.

```bash
git clone https://github.com/Shamanalle/despawn-indicator.git
cd despawn-indicator
./gradlew :common:test             # быстро: протокол, отсчёт, тексты, переводы
./gradlew build                    # все версии, два jar в build/libs/
./gradlew :fabric-1.21.2:runClient   # или :fabric-1.17, :fabric-1.18, :fabric-1.19, :fabric-1.19.4, :fabric-1.20, :fabric-1.20.2, :fabric-1.20.5, :fabric-1.21, :fabric-1.21.9, :fabric-26, :forge-1.20.1, :neoforge-1.21; runServer для сервера
```

### Где какой код

| Папка | Куда попадает | Что внутри |
|---|---|---|
| `common/` | во все jar (Java 17, без классов Minecraft) | протокол клиент–сервер (`ClockMessage`, `HelloMessage`, `Bytes`), отсчёт (`ClientClocks`), кому что отправлять (`ClockSender`), мигание (`Blink`), текст времени (`TimeText`), тексты сервера (`ServerText`), **переводы и иконка** |
| `common-j8/` | `fabric-1.17`, общие jar | те же исходники в байткоде Java 8 (Jabel): полоса 1.17.1 (Java 16) собирается с ними, а общие jar хранят их один раз для всех полос |
| `shared/mc/` | во все полосы Fabric, Forge и NeoForge | сервер (`ServerClocks`: что и когда отправлять; `StatusCommand`), тик клиента (`ClientTicker`) |
| `shared/fabric/` | в полосы Fabric | точки входа и события Fabric API, `Platform` / `ClientPlatform` для отправки |
| `shared/fabric-command-v1/` | `fabric-1.17`, `fabric-1.18` | `ModCommands`: регистрирует `/despawnindicator` через командный API v1 из Fabric API, единственный до 1.19 |
| `shared/fabric-command-v2/` | в полосы Fabric с 1.19 | `ModCommands`: то же через командный API v2 |
| `shared/mc-1.20/` | в полосы Fabric с 1.20, `fabric-26`, `forge-1.20.1`, `neoforge-1.21` | `Compat`: вызовы, у которых в 1.19.4 или 1.20 изменились имена или типы (тексты, мир игрока, ответы команды, владелец предмета) |
| `shared/fabric-1.17/` | `fabric-1.17`, `fabric-1.18`, `fabric-1.19`, `fabric-1.19.4` | каналы, отрисовка предметов и HUD для 1.17.1 – 1.19.4 (`PoseStack` и `GuiComponent` вместо `GuiGraphics`) |
| `shared/mc-1.17/` | `fabric-1.17`, `fabric-1.18` | `Compat` и `ClientCompat`: тексты и звук ноты для 1.17.1 – 1.18.2 |
| `shared/mc-1.19/` | `fabric-1.19`, `fabric-1.19.4` | `ClientCompat`: звук ноты для 1.19.x |
| `shared/gametest/` | `fabric-1.21.2`, `fabric-1.21.9`, `fabric-26` (клиентский gametest) | бросает предметы в настоящем мире, проверяет их время и делает скриншоты |
| `versions/fabric/<полоса>/` (`1.17`, `1.18`, `1.19`, `1.19.4`, `1.20`, `1.20.2`, `1.20.5`, `1.21`, `1.21.2`, `1.21.9`, `26`) | jar для Fabric / Quilt; проект Gradle `:fabric-<полоса>` | два канала и язык игрока (`ModNetworking`, `client/ClientNetworking`: до 1.20.5 каналы по имени, с 1.20.5 пакеты-payload, в 26.x другие имена), отрисовщик предметов и подключение к HUD (менялись в 1.21, 1.21.2 и 1.21.9), `fabric.mod.json` |
| `versions/forge/1.20.1/`, `versions/neoforge/1.21/` | jar для Forge / NeoForge; проекты `:forge-1.20.1`, `:neoforge-1.21` | обработчики Forge и NeoForge (`forge/`, `neoforge/`: вход, сеть на тех же каналах и байтах, клиент), `Platform` со временем жизни предметов, `mods.toml` / `neoforge.mods.toml`. Forge 1.20.1 работает с именами SRG, поэтому эта полоса перемаппится `reobfJar` |
| `universal/` | в оба jar | диспетчеры, выбирающие полосу при запуске (`BandAdapter` для Fabric, `ForgeDispatcher` / `NeoForgeDispatcher`) |
| `packaging/` | только сборка | `MergeJars`: собирает jar полос в один, общие классы кладёт один раз, а собственные классы каждой полосы переносит в `b/<полоса>/` |

Правила:
- Логика без Minecraft живёт в `common` и покрывается юнит-тестом.
- У Fabric 1.17.1 и 1.18.2 по своей полосе с одними и теми же исходниками: тексты, звук ноты и командный API изменились в 1.19, остальное — код 1.19.x. Полоса 1.17.1 для Java 16, поэтому собирается с `common-j8` вместо `common`. У Fabric 1.19.x две полосы, разделённые там, где изменился `ItemEntity.getOwner()`: 1.19 – 1.19.3 (компилируется против 1.19.2, потому что `PoseStack.translate(float, float, float)` есть только с 1.19.3) и 1.19.4. Их клиентский код лежит в `shared/fabric-1.17/`, и у каждой свой `Compat`; полосы с 1.20 используют общий из `shared/mc-1.20/`. У Fabric 1.20.x три полосы, разделённые там, где менялась сеть: 1.20 – 1.20.1 (сервер не узнаёт язык игрока), 1.20.2 – 1.20.4 (каналы по имени) и 1.20.5 – 1.20.6 (payload, Java 21). У Fabric 1.21.x три, разделённые там, где менялась отрисовка предметов: 1.21 – 1.21.1, 1.21.2 – 1.21.8 и 1.21.9 – 1.21.11. Каждая компилируется против последней версии своего диапазона (gradle.properties) и работает на всех. Прежде чем использовать в `shared/` новый метод Minecraft или Fabric API, проверьте, что его сигнатура одинакова во всех выпусках, которые покрывают полосы; `.github/workflows/compat.yml` проверяет jar каждой полосы на каждом выпуске её диапазона. **Нестабильные** примеры: `Entity.level()`, `Entity.position()`, `GameProfile.getName()`, `ServerLevel.getEntity(UUID)`, `SoundManager.play`, `ItemEntity.getOwner()`.
- Клиент и сервер обмениваются сырыми байтами на `despawnindicator:hello` (клиент → сервер) и `despawnindicator:clocks` (сервер → клиент). Все загрузчики и будущий плагин пишут ровно одни и те же байты; `ClockMessageTest` их фиксирует.
- Сервер отправляет только игрокам, которые поздоровались, в конце тика сервера, поэтому пачка выпавших предметов уходит одним пакетом.

### Переводы

Файлы языков лежат в `common/src/main/resources/assets/despawnindicator/lang/` (13 языков). Каждый новый ключ добавляйте во **все** файлы. `LangConsistencyTest` падает, если ключа из кода нет, если у языков разные ключи или разные плейсхолдеры.

### Документация

README состоит из двух файлов: `README.md` на английском и `README.ru.md` на русском. Разделы в них одинаковые, меняются они вместе. Остальная документация (CHANGELOG, этот файл, шаблоны issue и pull request) пишется сначала по-английски, затем полностью по-русски. README — справочник для игроков и владельцев серверов, без внутреннего устройства.

Заметки к релизам на GitHub собираются из `CHANGELOG.md` скриптом `.github/scripts/release-notes.sh`. Когда журнал меняется в `main`, workflow *Sync release notes* обновляет заметки уже опубликованных релизов.

Описание репозитория и темы лежат в `.github/about.json`. Workflow *Repository about* проверяет файл в pull request и применяет его, когда он меняется в `main`. Для применения нужен секрет `REPO_ADMIN_TOKEN`, потому что встроенный токен workflow не может менять настройки репозитория. Настраивается один раз:
1. GitHub → *Settings → Developer settings → Personal access tokens → Fine-grained tokens → Generate new token*.
2. *Repository access*: *Only select repositories* → этот репозиторий. *Permissions → Repository permissions → Administration*: *Read and write*. Больше ничего не нужно.
3. В этом репозитории: *Settings → Secrets and variables → Actions → New repository secret*, имя `REPO_ADMIN_TOKEN`, значение — токен.
4. Один раз запустите *Actions → Repository about → Run workflow* или измените `.github/about.json`. Когда срок токена истечёт, создайте новый и обновите секрет.

### Проверки в игре

*In-game tests* (`.github/workflows/ingame.yml`) запускается на каждый pull request и перед каждым релизом:
- **Серверы:** `.github/scripts/server-smoke.sh` запускает серверы Fabric, Forge и NeoForge с модом, выполняет `/despawnindicator` из консоли и падает, если ответа нет, если вместо текста ключи или если мод бросил исключение. Локально: `bash .github/scripts/server-smoke.sh fabric 1.21.1 build/libs/despawn-indicator-fabric-*+mc1.17-26.x.jar`.
- **Клиент:** `./gradlew :fabric-1.21.2:runClientGameTest` (или `:fabric-1.21.9`, `:fabric-26`) создаёт мир, бросает алмаз, который вот-вот исчезнет, изумруд, который не исчезает никогда, и золотой слиток, проверяет полученное клиентом время, выполняет `/despawnindicator` и сохраняет скриншоты в `build/run/clientGameTest/screenshots/`. В CI он идёт под `xvfb-run` с программными OpenGL и Vulkan из Mesa; клиенты 1.21.x работают на Java 21.
- Логи и скриншоты прикладываются к каждому запуску как артефакты.
- **Сбои сети:** загрузка, сорвавшаяся по дороге (таймаут, разрыв соединения, HTTP 429 / 5xx), повторяется один раз и оставляет предупреждение: сборка и клиент до запуска игры (`.github/scripts/net-retry.sh`, второй раз — Maven Central через зеркало Google), установщик сервера и его первый запуск. Упавший тест, ошибка компиляции и отсутствующий файл (404) сразу дают ошибку.

*Build & Verify* ещё компилирует полосы Forge и NeoForge против самого старого заявленного загрузчика, а *Fabric compatibility* проверяет, что jar каждой полосы Fabric линкуется с каждым выпуском её диапазона (`LinkCheck.java`).

### Релизы

1. Укажите `mod_version` в `gradle.properties` и добавьте раздел в `CHANGELOG.md` (сначала по-английски, затем по-русски).
2. Влейте в `main` и дождитесь зелёной сборки. Релиз ещё раз прогоняет проверки в игре и останавливается, если они упали.
3. *Actions → Publish Release → Run workflow* на `main` или отправьте тег `vX.Y.Z`. Workflow соберёт все jar, создаст тег и релиз и возьмёт заметки из `CHANGELOG.md`.
4. Тот же запуск загрузит файлы на Modrinth и CurseForge (см. ниже). Загрузить уже вышедший релиз ещё раз: *Actions → Publish to Modrinth & CurseForge → Run workflow* с его тегом.

### Modrinth и CurseForge

Workflow *Publish to Modrinth & CurseForge* (`.github/workflows/publish.yml`) берёт два файла релиза на GitHub и загружает их в оба магазина. Пока нет id проекта или токена, магазин пропускается с предупреждением. Настройка, один раз:
1. Создайте проекты вручную: Modrinth → *Create a project* (тип *Mod*); CurseForge → *Minecraft → Mods → Create project*. Название, краткое описание, категории и чек-лист — в `docs/store-description.md`. Оба сайта проверяют новые проекты; первую загрузку можно сделать, пока проект ждёт проверки. Страница Modrinth — это `docs/modrinth.md`, её загружает workflow *Modrinth page*.
2. Впишите id проектов в `.github/publish.json`: на Modrinth — *Project ID* из меню проекта, на CurseForge — число *Project ID* на странице проекта.
3. Токены: Modrinth → *Settings → Personal access tokens*, права *Create versions*, *Read projects*, *Write versions* (чтобы *Sync release notes* обновлял описание изменений у версий на Modrinth) и *Write projects* (чтобы *Modrinth page* загружал страницу и галерею); CurseForge → *Account → API tokens*. Добавьте их как секреты репозитория `MODRINTH_TOKEN` и `CURSEFORGE_TOKEN` (*Settings → Secrets and variables → Actions*). Токены никогда не попадают в файлы, коммиты и чаты.

### Pull request

1. Ветка от `main` (`feature/…`, `fix/…`).
2. Запустите `./gradlew :common:test` и `./gradlew build`.
3. Опишите, что изменилось и как проверяли в игре (версии Minecraft и загрузчика; клиент, сервер или оба; с шейдерами или без).
4. Добавьте запись в `CHANGELOG.md` в раздел *Unreleased*, на английском и на русском. Журнал изменений становится заметками к релизу на Modrinth и CurseForge: короткие простые пункты о том, что заметят игроки и владельцы серверов, без внутреннего устройства.
