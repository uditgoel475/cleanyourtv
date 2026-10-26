---
generated-by: /ai-sdlc/memorize
generated-at: 2026-10-26
---

# Hotspots and Caution Zones Memory

## High-Churn Files (modify carefully, test thoroughly)

Detected via `git log --format="" --name-only --since="6 months ago"`, `examples/` excluded. The clone is not
shallow (`git rev-parse --is-shallow-repository` printed `false`), but the history spans only 2026-10-17 to
2026-10-26 (2733 commits), so "6 months" is the whole history. Counts are touches per file.

| File | Commits (6 mo) | Notes |
|------|----------------|-------|
| `README.md` | 531 | Generated tables and counts; never hand-edit them (`tools/gen_tables.py`) |
| `SHOWCASE.md` | 180 | Union-merged; one short sentence per row, at the bottom of its section |
| `launcher/app/src/main/java/com/gathin/tvhome/MainActivity.java` | 151 | Home path; changes usually held for a real-TV run |
| `themes/README.md` | 103 | Keys table must match `Config.java` and `docs/switches.md` |
| `docs/TROUBLESHOOTING.md` | 98 | Facts pinned by tests |
| `launcher/app/src/main/res/values/config.xml` | 94 | New keys default off |
| `launcher/app/src/main/java/com/gathin/tvhome/Config.java` | 79 | Every key optional, bad types ignored |
| `launcher/app/src/main/java/com/gathin/tvhome/AppAdapter.java` | 42 | Implements `AppAdapter.TileListener` for `MainActivity` |
| `launcher/app/src/main/res/values/strings.xml` | 39 | Every addition needs all 27 `values-*` folders |
| `PROMPT.md` | 38 | Safety rules; changes need the maintainer |
| `addons/README.md` | 35 | Add-on table, one cell per add-on |
| `launcher/app/src/main/AndroidManifest.xml` | 34 | Permissions and exported components; no new permissions |
| `launcher/build-and-install.sh` | 28 | Home guard; changes need a dry run on a real TV |
| `tools/readme_stats.py` | 28 | Draws README card |
| `launcher/app/src/main/java/com/gathin/tvhome/HomeKeyService.java` | 23 | Remote Home key routing |

## Complexity Indicators

Java and Python: `(Get-Content).Count` (all lines). Shell scripts: non-blank lines (`Measure-Object -Line`).

| File | Lines | Recommendation |
|------|-------|---------------|
| `launcher/app/src/main/java/com/gathin/tvhome/MainActivity.java` | 3546 | Do not add features here; extract a package-private helper and test it |
| `launcher/app/src/main/java/com/gathin/tvhome/AppAdapter.java` | 952 | Check D-pad from every edge after any change |
| `launcher/app/src/main/java/com/gathin/tvhome/Config.java` | 884 | Add keys the way `show_stats` is read; keep APK growth under ~1 KB |
| `launcher/app/src/main/java/com/gathin/tvhome/Cards.java` | 864 | Untrusted input: no regex, no throwing static init |
| `launcher/app/src/main/java/com/gathin/tvhome/AppLibraryDialog.java` | 680 | Part of the opt-in app library (`app_library` in `config.xml`) |
| `launcher/app/src/main/java/com/gathin/tvhome/WallpaperPlayer.java` | 673 | Cap bitmaps at 1920 px, no blur or video |
| `tools/bench.py` | 606 | Perf measurement (`docs/workflows/perf-ab.md`) |
| `tools/recovery.py` | 429 | Holds `CORE_RE`/`CORE_OK` copy; keep equal to shell scripts |
| `tools/manage.py` | 397 | Reads `themes/README.md` and add-on tables |
| `tools/check.py` | 390 | Refuses core-package lists even with `--fix` |
| `tools/disable.sh` | 283 (non-blank) | Safety guards; edit with `disable.ps1` twin |
| `launcher/build-and-install.sh` | 217 (non-blank) | Home handover; edit with `.ps1` twin |

## Tech Debt Notes

- TODO/FIXME/HACK/XXX: 0 in `launcher/app/src/main/java`, `tools/*.py` and `tests/*.py` (case-sensitive `\b(TODO|FIXME|HACK|XXX)\b`). A case-insensitive search gives false hits ("swapIntoDock", "tvvibehack", "todo" variable in `check.py`).
- `MainActivity.java` is a 3546-line God class with many `private` fields toggled by config keys.
- Safety regex `CORE_RE`/`CORE_OK` is copied into `disable.sh`, `disable.ps1` and `recovery.py`; drift is only caught by `tests/test_core_packages.py`.
- 5 tracked scripts sit inside list folders (`examples/anker-nebula-d2426/{build-and-install.sh,restore.sh}`, `examples/epson-ls800/restore.sh`, `examples/tcl-43c645/{build-and-install.sh,restore.sh}`) against the current rule; do not copy them.
- Coverage is not measured anywhere (see `test-conventions.md`).
- Every shell script has a PowerShell twin that CI cannot run (CI has no PowerShell).

## Safe Extension Points

| Extension Point | How to Use |
|----------------|-----------|
| `examples/<folder>/` | Add `debloat.txt` and `tv.json`; nothing else outside the folder (`docs/workflows/tv-list.md`) |
| `themes/<name>/config.json` | Existing keys only, plus `screenshot.jpg` (`docs/workflows/theme.md`) |
| `addons/<name>/` | Own Gradle project and module card, no change under `launcher/` (`docs/workflows/addon.md`) |
| Module card (`docs/specs/card.md`) | Declare `com.gathin.tvhome.category.DOCK_CARD` and `meta-data com.gathin.tvhome.card` on the LEANBACK_LAUNCHER activity |
| `config.xml` key | Off by default, read in `Config.java`, documented in `themes/README.md` and `docs/switches.md` |
| New locale | `docs/workflows/translation.md`; register in `locales_config.xml` |
| `SHOWCASE.md` row | Bigger ideas live in the contributor's own repo (`docs/workflows/showcase.md`) |
