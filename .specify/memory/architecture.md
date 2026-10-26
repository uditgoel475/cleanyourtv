---
generated-by: /ai-sdlc/memorize
generated-at: 2026-10-26
domain: swe
---

# Architecture Memory

This repo is not a Spring Boot service. It is an Android TV debloat kit: a prompt, ADB tooling (Python and
shell), 300 tested TV lists, and one small native launcher in plain Java. Source: `AGENTS.md` (Repo map).

## Layer Map

| Layer | Directory / Module | Responsibility |
|-------|--------------------|----------------|
| Prompt | `PROMPT.md` | Brand-neutral procedure a coding agent follows on a real TV; holds the safety rules |
| Debloat lists | `examples/<brand-model>/` (300 folders) | `debloat.txt`, `tv.json`, optional `README.md` and `config.json` per tested TV |
| Device notes | `docs/devices/` | Models with notes but no tested list |
| ADB client | `tools/tv.py`, `tools/b64push.py`, `tools/relay.py` | ADB-over-TCP via `adb_shell`, no `adb` binary |
| Apply and undo | `tools/disable.sh`/`.ps1`, `tools/restore.sh`/`.ps1`, `tools/recovery.py`, `tools/check.py` | Disable, restore, record state, detect firmware drift; core-package and Home guards |
| Launcher | `launcher/app/src/main/java/com/gathin/tvhome/` (63 `.java` files) | Home screen, package `com.gathin.tvhome`, `minSdk` 26, no `INTERNET` permission |
| Launcher install | `launcher/build-and-install.sh`/`.ps1` | Only way in: build, install, hand over Home, enforce Home guard |
| Themes | `themes/<name>/` (9 folders) | `config.json` overlay on `config.xml`, required `screenshot.jpg` |
| Add-ons | `addons/<name>/` (20 folders) | Each its own Gradle project; module cards; never touches `launcher/` |
| Perf tooling | `tools/bench.py`, `trace.py`, `trace_report.py`, `flamechart.py`, `screendiff.py` | Measure launcher regressions (optional workflow) |
| Generated docs | `tools/gen_tables.py`, `readme_*.py` | README TV tables and counts come from `examples/*/tv.json` |
| Tests | `tests/` (129 `test_*.py`), `launcher/app/src/test/` (38 Java files) | Python `unittest` for lists, docs, tools; JUnit 4 for launcher logic |
| CI | `.github/workflows/ci.yml`, `addons.yml`, `maintainer-bot.yml` | Tests, launcher build, APK size gate |
| Review policy | `.github/maintainer/MAINTAINER.md`, `docs/DOS-AND-DONTS.md` | What reviewers accept, send back or close |

## Entry Points

| Entry Point | File | Description |
|-------------|------|-------------|
| Home activity | `launcher/app/src/main/java/com/gathin/tvhome/MainActivity.java` | `HOME` + `LEANBACK_LAUNCHER` filters; dock, app grid, glass highlight; reads `Config` once in `onCreate` |
| Settings | `LauncherSettingsActivity.java`, `QuickSettingsActivity.java`, `HomeSettingsActivity.java` | On-TV settings, remote Settings panel, stock-Home switch |
| Remote helpers | `HomeKeyService.java`, `InputKeyService.java` | Accessibility services that route remote keys; enabled over ADB |
| Inputs | `InputsRequestActivity.java` | Disabled by default; enabled only while `handle_view_inputs` is true |
| Screensaver | `ClockActivity.java`, `ClockDream.java` | Idle clock |
| Theme delivery | `ThemeProvider.java` (ContentProvider), `ThemeFiles.java` | Reads pushed theme files |
| Guard | `GuardConfig.java`, `GuardKeep.java`, `GuardRevival.java`, `ServiceRepair.java` | Keep helpers enabled after a force stop |
| ADB client CLI | `tools/tv.py` | `TV_HOST=<ip> tools/tv.py --probe` or `<shell command>` |
| Apply a list | `tools/disable.sh` / `disable.ps1` | One package per `pm disable-user --user 0` call |
| Undo a list | `tools/restore.sh` / `restore.ps1` | Reverses a list |
| Install launcher | `launcher/build-and-install.sh` / `.ps1` | Build, install, set Home |
| Tests | `python3 -m unittest discover -s tests -v`; `./gradlew testReleaseUnitTest` in `launcher/` | See `test-conventions.md` |

## Architectural Pattern

Flat, offline, single-module Android app plus script toolbox. All launcher classes are in one package. Most
are package-private `final class` helpers holding testable pure logic (`DockSlots`, `Order`, `GridNavigation`,
`Cards`, `MemoryTrimPolicy`), called from a large `MainActivity`. Device-specific behaviour is data, not
code: `config.xml` keys, overridden by a theme `config.json`. Safety logic lives in scripts and is shared by
copy, so `CORE_RE` and `CORE_OK` must stay identical in `disable.sh`, `disable.ps1` and `recovery.py`
(`tools/disable.sh`, comment above `CORE_RE`; checked by `tests/test_core_packages.py`).

## Key Design Decisions (observed)

- **Disable, never uninstall**: `pm disable-user --user 0`; `AGENTS.md` (Cleaning a TV), `tools/disable.sh` header.
- **Small core**: APK gate 589,824 B in `.github/workflows/ci.yml`; R8 `minifyEnabled` and `shrinkResources` in `launcher/app/build.gradle`.
- **Config as data**: `Config.java` merges `res/values/config.xml` with `config.json` in external files dir; unknown keys ignored (class Javadoc in `Config.java`).
- **No regex in untrusted-input code**: `Cards.java` class Javadoc (ICU vs JVM regex differences).
- **Every module card read in its own try**: `Cards.java` and `CardFeature.java`; a bad card is dropped with one log line.
- **Home guard**: stock launcher is disabled only after the new launcher is confirmed as Home; enforced in `disable.sh` and `build-and-install.sh`, tested in `tests/test_home_guard.py`.
- **Generated tables**: README TV tables and counts come from `examples/*/tv.json` via `tools/gen_tables.py`.
- **Release signing**: release build is signed with the debug key and sideloaded, never published (`launcher/app/build.gradle`).
