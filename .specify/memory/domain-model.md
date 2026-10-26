---
generated-by: /ai-sdlc/memorize
generated-at: 2026-10-26
---

# Domain Model Memory

No database, no JPA, no migrations. The "entities" are files and Android resources.

## Core Entities

| Entity | File | Key Fields | Relationships |
|--------|------|-----------|---------------|
| Debloat list | `examples/<folder>/debloat.txt` | one package per line, `#` comments, trailing `# note` | applied by `tools/disable.sh`, undone by `tools/restore.sh` |
| TV row | `examples/<folder>/tv.json` | `tv`, `os`, `brand`, `notes`, optional `readme_os` | feeds `tools/gen_tables.py` -> `README.md`, `examples/README.md` |
| TV config | `examples/<folder>/config.json` (optional) | theme-style keys for that TV | pushed with `tools/theme.sh` |
| Launcher config | `launcher/app/src/main/res/values/config.xml` | `dock_apps`, `fill_dock`, `hidden_apps`, `live_tv_package`, `columns`, ... | overlaid by theme `config.json`; read by `Config.java` |
| Theme | `themes/<name>/config.json`, `screenshot.jpg`, optional `wallpapers/` | same keys as `config.xml` (not `input_sources`) | `tools/theme.sh`, `tools/manage.py`; keys documented in `themes/README.md` |
| Launcher settings | SharedPreferences `launcher_settings` (`Config.SETTINGS_NAME`) | `settings_revision`, `wallpaper_choice`, `rows_below_dock`, `dock_opacity`, ... | take priority over a theme until "Follow theme" |
| Module card | `res/xml/module_card.xml` in a third-party app | `spec="1"`, `label` (1-24), `caption` (<=32), `accent` `#RRGGBB` | discovered by `Cards.java`; decision pinned to signer certificate |
| Recovery record | `~/.cleanyourtv/recovery-<tag>-<digest>.json` (`tools/recovery.py`), one per TV and list | state a list is about to change: disabled set, Home owner, animation scales | read by `restore.sh` via `recovery.py --commands` |
| Add-on | `addons/<name>/` | own Gradle project, `README.md`, `screenshot.jpg` | declares a module card; listed in `addons/README.md` |
| Switch screenshot | `docs/switches/<key>.jpg` | one per Keys row | required by `tests/test_switches.py` |

## Schemas / Contracts

| Schema | File | Used By |
|--------|------|---------|
| Module card spec 1 | `docs/specs/card.md`, `docs/modules.md` | `Cards.java`, `CardFeature.java`, add-ons |
| Theme keys table | `themes/README.md` (Keys) | `Config.java`, `tests/test_theme_keys_*.py`, `tests/test_theme_guide_keys.py` |
| `tv.json` | `CONTRIBUTING.md` section 1 | `tools/gen_tables.py`, `tests/test_tv_json_recipe_keys.py` |
| Core package regex `CORE_RE`/`CORE_OK` | `tools/disable.sh`, `tools/disable.ps1`, `tools/recovery.py` | refusal of unsafe lists; `tests/test_core_packages.py` |
| Card limits | `Cards.java` (`MAX_DECLARED` 32, `MAX_SHOWN` 8, `MAX_RECORDS` 128, `LABEL_MAX` 24, `CAPTION_MAX` 32) | module card validation |
| Android manifest | `launcher/app/src/main/AndroidManifest.xml` | permissions, HOME filter, exported components |

## State Machines (if any)

| Entity | States | Transitions |
|--------|--------|------------|
| Package on TV | enabled -> disabled-user | `pm disable-user --user 0` (disable); `restore.sh` replays the recorded state (packages already disabled before stay disabled) |
| Home owner | stock launcher -> this launcher | stock disabled only after this launcher is confirmed Home |
| Module card | undecided -> shown / hidden | owner answers a prompt once; relabel in an update asks again |
| Remote helper | enabled -> disabled by force stop -> repaired | `ServiceRepair` re-enables via `WRITE_SECURE_SETTINGS` |

## Bounded Contexts (if identifiable)

Four contexts sharing one repo: (1) TV cleaning (prompt, lists, tools), (2) the launcher, (3) the content hub
(themes, add-ons, showcase), (4) project docs and generated README. They meet only through `config.xml` keys,
`tv.json` and the install scripts.

## Data Flow

`examples/*/tv.json` -> `tools/gen_tables.py` -> README tables and counts.
`debloat.txt` -> `disable.sh` (shape, core-package, Home and setupwraith guards) -> `tv.py` -> `pm disable-user`.
`themes/<name>/config.json` -> `theme.sh` -> TV external files dir -> `Config.java` overlay -> `MainActivity`.
