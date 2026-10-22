# Store pages / Страницы на площадках

The Modrinth project page is `docs/modrinth.md`: the whole page, in English, with the Russian translation collapsed in a `<details>` block at the end. It is short and tells players what they get; commands and details belong in the README. The *Modrinth page* workflow uploads the page and the summary below to Modrinth on every change pushed to `main` (it needs the `MODRINTH_TOKEN` secret with the *Write projects* scope). CurseForge cannot be updated this way: paste the same text into the description editor by hand. The gallery pictures are listed in `docs/gallery.json` (file, title, description, featured; the list's order is their order) and the same workflow puts them into the Modrinth gallery, replacing the picture with the same title. The list is empty until the first release brings screenshots from the in-game client test. Each picture must stay under 5 MiB.

The page describes the first release (0.1.0) and is uploaded only once the project exists and its id is in `.github/publish.json`.

Страница проекта на Modrinth — это `docs/modrinth.md`: вся страница на английском, а русский перевод свёрнут в блок `<details>` в конце. Она короткая и рассказывает игрокам, что они получат; команды и подробности — в README. Workflow *Modrinth page* выкладывает страницу и краткое описание ниже на Modrinth при каждом изменении в `main` (нужен секрет `MODRINTH_TOKEN` с правом *Write projects*). CurseForge так обновить нельзя: вставьте тот же текст в редактор описания вручную. Картинки галереи перечислены в `docs/gallery.json` (файл, заголовок, описание, featured; порядок списка — порядок картинок), тот же workflow кладёт их в галерею Modrinth, заменяя картинку с тем же заголовком. Пока не вышел первый релиз со скриншотами из клиентского игрового теста, список пуст. Каждая картинка должна быть меньше 5 МиБ.

Страница описывает первый релиз (0.1.0) и выкладывается, только когда проект создан и его id записан в `.github/publish.json`.

---

## English

**Name:** Despawn Indicator: Item Despawn Timer

No loader, Minecraft version or other mods' names in the name: the stores show those as tags, and the name stays right as versions are added. "Item Despawn Timer" is what players search for.

**Summary:** Dropped items blink before they despawn and show how long they have left. Works client-only with an estimate, exact with the mod on the server. Shader-friendly blinking; respects Forge and NeoForge item lifespans.

213 characters: Modrinth allows 256, CurseForge 250. It names what players see first (blinking, time left), then why it is better than a plain client mod (exact with the server half, works with shaders, modded lifespans).

**Categories:** Utility, Game Mechanics

**Environment:** client and server, each optional (Modrinth: *Client: optional, Server: optional*).

**Dependencies:** Fabric API (required on Fabric / Quilt), Mod Menu (optional).

**Plugin:** the Paper / Folia / Spigot plugin is a version of the same Modrinth project (loaders Paper, Folia, Spigot, Bukkit, Purpur). CurseForge keeps Bukkit plugins in a separate kind of project, so the plugin is not uploaded there.

**License:** MIT.

**Links:** source `https://github.com/Shamanalle/despawn-indicator`, issues `https://github.com/Shamanalle/despawn-indicator/issues`.

**Icon:** `docs/images/icon.png` (512×512).

### Upload checklist

1. Create the project on Modrinth (type *Mod*) and on CurseForge (*Minecraft → Mods*) with the name, summary, categories, icon, license and links above.
2. Modrinth: set the environment as above. CurseForge: paste the text of `docs/modrinth.md` (English only) into the description.
3. Put both project ids into `.github/publish.json` and add the `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN` secrets (scopes in `CONTRIBUTING.md`).
4. Run *Actions → Modrinth page* once, then publish the release.

## Русский

**Название:** Despawn Indicator: Item Despawn Timer

В названии нет загрузчика, версии Minecraft и названий других модов: площадки показывают их тегами, а название не придётся менять при добавлении версий. «Item Despawn Timer» — то, что ищут игроки.

**Краткое описание** (на площадках только на английском, как выше). Перевод: Выброшенные предметы мигают перед исчезновением и показывают, сколько им осталось. Работает только на клиенте с оценкой, точно — с модом на сервере. Мигание дружит с шейдерами; учитывается время жизни предметов из Forge и NeoForge.

**Категории:** Utility, Game Mechanics

**Окружение:** клиент и сервер, оба необязательны (Modrinth: *Client: optional, Server: optional*).

**Зависимости:** Fabric API (обязателен на Fabric / Quilt), Mod Menu (необязателен).

**Плагин:** плагин для Paper / Folia / Spigot — версия того же проекта на Modrinth (загрузчики Paper, Folia, Spigot, Bukkit, Purpur). CurseForge держит плагины Bukkit в отдельном виде проектов, поэтому туда плагин не загружается.

**Лицензия:** MIT.

**Ссылки:** исходники `https://github.com/Shamanalle/despawn-indicator`, issues `https://github.com/Shamanalle/despawn-indicator/issues`.

**Иконка:** `docs/images/icon.png` (512×512).

### Чек-лист загрузки

1. Создайте проект на Modrinth (тип *Mod*) и на CurseForge (*Minecraft → Mods*) с названием, кратким описанием, категориями, иконкой, лицензией и ссылками выше.
2. Modrinth: укажите окружение, как выше. CurseForge: вставьте в описание текст `docs/modrinth.md` (только английскую часть).
3. Впишите id обоих проектов в `.github/publish.json` и добавьте секреты `MODRINTH_TOKEN` и `CURSEFORGE_TOKEN` (права — в `CONTRIBUTING.md`).
4. Один раз запустите *Actions → Modrinth page*, затем опубликуйте релиз.
