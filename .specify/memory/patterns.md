---
generated-by: /ai-sdlc/memorize
generated-at: 2026-10-26
---

# Code Patterns Memory

Stack: Java 17 (Android, no Kotlin/Compose), Python 3.12 (stdlib `unittest`), bash and PowerShell twins.
The Spring Boot rules in `.github/copilot-instructions.md` do not apply here (see `house-rules.md`, Conflicts).

## Naming Conventions

| Thing | Convention | Example |
|-------|-----------|---------|
| Launcher class | PascalCase, package-private `final class` unless Android needs it public | `final class DockSlots`, `public class MainActivity` |
| Launcher test | `<Class>Test` in the same package | `DockSlotsTest` |
| Python script | lowercase, `snake_case.py`; `adb-closed.py` uses a hyphen | `trace_report.py` |
| Shell script | `kebab-case.sh` with a `.ps1` twin | `build-and-install.sh` / `build-and-install.ps1` |
| Python test file | `test_<topic>.py` | `test_home_guard.py` |
| List folder | `examples/<brand>-<model>/` lowercase, hyphens | `examples/epson-ls800/` |
| Theme / add-on folder | lowercase, hyphens | `themes/six-columns/`, `addons/photo-screensaver/` |
| Config key | `snake_case` in `config.xml`, same name in theme JSON | `fill_dock`, `live_tv_package` |
| Commit/PR title | `area: what changed (#issue)` | `tools: check.py refuses a command line it cannot run instead of reporting drift` |

## Framework & Library Patterns

### Launcher logic as pure package-private helpers

```java
// DockSlotsTest.java: logic is static, takes plain collections, and runs on the JVM with no Android
List<String> keys = Arrays.asList("tivimate", "jellyfin", "iplayer", "appletv", "my5", "youtube");
Set<String> dock = set("tivimate", "jellyfin", "iplayer", "appletv");
assertEquals(keys, DockSlots.dockFirst(keys, 4, dock));
```

### Config key flow (off by default)

`res/values/config.xml` key (commented, default off) -> read in `Config.java` from resource and from theme JSON ->
row in `themes/README.md` Keys table -> row and `docs/switches/<key>.jpg` in `docs/switches.md`.
Source: `docs/workflows/launcher-switch.md`.

### Python tools with a fake transport

Tests replace `tools/tv.py` with a fake that answers queries from a JSON file (`FAKE_TV_STATE`), and run the
real shell script against it (`tests/test_home_guard.py`).

## Error Handling Pattern

- Untrusted input never crashes Home: each module card is parsed in its own `try`, one log line (`TvHomeCards`), card dropped (`Cards.java`).
- Theme `config.json`: an unknown or badly typed key is ignored; an unparseable file is ignored as a whole (`themes/README.md` Keys).
- Scripts: `set -euo pipefail`, refuse early with a message to stderr and exit 1 before touching the TV (`tools/disable.sh`).
- Logging uses `android.util.Log` with a `TvHome.*` tag (`Config.TAG = "TvHome.Config"`).

## Async Pattern

Launcher is mostly main-thread UI. Heavy work (hashing, decoding) must stay off the main thread and bitmaps are
capped at 1920 px (`docs/DOS-AND-DONTS.md`, Launcher switches). `HandlerThread` is imported in `MainActivity.java`. No background services or polling.

## Dependency Injection / Wiring Pattern

None. No DI framework. Helpers are static or constructed in `MainActivity.onCreate`; `Config` is read once there.

## When Adding a New Feature - Follow This Pattern

1. Pick the home first: PR here, `themes/`, `addons/`, or a fork plus `SHOWCASE.md` (`AGENTS.md`, Where a change goes).
2. For a launcher switch, follow `docs/workflows/launcher-switch.md`: off by default, strings in every `values-*` folder or config-only.
3. Put logic that needs no TV in a package-private class and add a JUnit test in `launcher/app/src/test/java/com/gathin/tvhome/`.
4. For a TV list, follow `docs/workflows/tv-list.md`: folder with `debloat.txt` and `tv.json` only; do not edit README tables.
5. For a script change, update the `.sh` and `.ps1` twin together and keep `CORE_RE`/`CORE_OK` identical in all three places.
6. Run `python3 -m unittest discover -s tests -v`, and in `launcher/` run `./gradlew --no-daemon testReleaseUnitTest lintRelease assembleRelease`; check the APK is under 589,824 B.
7. Verify on a real TV: `tools/tv.py --screenshot shots/after.png DPAD_DOWN`; D-pad from every edge.
