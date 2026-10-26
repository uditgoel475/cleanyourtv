---
generated-by: /ai-sdlc/memorize
generated-at: 2026-10-26
---

# Test Conventions Memory

Two test suites. Neither uses Spring, Testcontainers, Mockito or Robolectric.

## Framework

- **Launcher runner**: JUnit 4.13.2 (`testImplementation` in `launcher/app/build.gradle`), plain JVM, run by `testReleaseUnitTest`.
- **Tools/lists/docs runner**: Python stdlib `unittest` (0 `pytest` imports in `tests/`), 129 `test_*.py` files, 1339 indented `def test_` functions, 250 `unittest.TestCase` mentions.
- **Coverage tool**: [UNKNOWN - not configured]. No `jacoco`, `coverage` or `threshold` match in `launcher/build.gradle`, `launcher/app/build.gradle` or `.github/workflows/*.yml`; no `sonar-project.properties`.
- **Coverage threshold**: not configured. The 85% branch figure in `.github/copilot-instructions.md` has no enforcement here.
- **Lint gate**: `lintRelease` at zero errors (`launcher/app/lint.xml`, `.github/workflows/ci.yml`).
- **CI-command lock**: `tests/test_agents_md.py` asserts `ci.yml` and `AGENTS.md` both contain the unittest command, the gradle command and `CLEANYOURTV_STRICT`; changing `ci.yml` means updating `AGENTS.md` too.
- **Size gate**: release APK under 589,824 B (`.github/workflows/ci.yml`).

## Directory Structure

```
tests/                                   129 flat test_<topic>.py files, no subfolders
launcher/app/src/test/java/com/gathin/tvhome/   <Class>Test.java mirrors main package (37 files)
launcher/app/src/test/java/androidx/dynamicanimation/animation/SpringFrames.java   test helper
addons/<name>/                           each add-on has its own Gradle project and CI (addons.yml)
```

## Fixture / Factory Pattern

```python
# tests/test_home_guard.py: FAKE_TV replaces tools/tv.py and answers from a JSON state file
STATE_PATH = os.environ["FAKE_TV_STATE"]
def answer(command):
    if command == "pm list packages -d":
        return "".join(f"package:{name}\n" for name in state["disabled"])
```

Java tests build inputs inline with small private helpers (`DockSlotsTest.set(String...)`).

## Mock Strategy

- Java: 0 matches for `mockito|robolectric` in `launcher/app/src/test`. Logic is extracted into Android-free classes and tested directly.
- Python: the real shell script runs against a fake `tv.py` (23 test files mention `FAKE_TV`/`fake tv`). `unittest.mock` appears in 12 lines.
- PowerShell cases skip without `pwsh` (9 `skipIf`/`skipUnless`/`skip` lines); CI has no PowerShell.

## Typed Test Helpers (use these instead of casting)

Not applicable: Java and Python, no TypeScript. Reusable helpers actually present:

| Helper / type | Import | Use for | Files using it |
|---------------|--------|---------|----------------|
| `SpringFrames` | `androidx.dynamicanimation.animation` (test source set) | spring animation frames in motion tests | [UNKNOWN - not counted] |
| `FAKE_TV` template | defined per test module, e.g. `tests/test_home_guard.py` | simulate ADB for the real scripts | 23 files |

## Interaction and Library Versions (frontend)

Not applicable. Library majors: JUnit 4.13.2, `adb-shell==0.4.4`, Python 3.12 in CI.

## E2E Policy

No browser E2E. Real-TV verification is a manual PR requirement: `tools/tv.py --screenshot shots/after.png DPAD_DOWN`
and D-pad from every edge (`CONTRIBUTING.md` section 2, `AGENTS.md` Before opening a PR). Home or install-path changes
are usually held for a run on a real TV (`.github/maintainer/MAINTAINER.md`).

## Test Naming Convention

- Java: `@Test public void aSentenceInCamelCase()`; 242 `@Test` annotations, 244 `public void camelCase()` methods (two are helpers), 0 snake_case. Example: `aDockAppPushedPastTheDockLeavesIt` in `DockSlotsTest.java`. Comments cite the issue (`// #499: ...`).
- Python: `test_<a_full_sentence_with_underscores>`. Example: `test_the_owner_of_home_is_skipped_when_it_is_the_only_home` in `tests/test_home_guard.py`.
- Do not pin exact prose wording, and do not write always-true tests (`docs/DOS-AND-DONTS.md`, Tools).

## How to Run Tests

```bash
python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt
python3 -m unittest discover -s tests -v          # CLEANYOURTV_STRICT=1 on main in CI
cd launcher && ./gradlew --no-daemon testReleaseUnitTest lintRelease assembleRelease
python3 tools/gen_tables.py --check               # README tables match examples/*/tv.json (main only in CI)
```

## When Writing New Tests - Follow This Pattern

1. Launcher logic: extract it into a package-private class, add `<Class>Test` in the same package, name each test as a sentence, cite the issue in a comment.
2. Scripts: run the real `.sh` against a fake `tv.py` driven by a state file; do not mock the script.
3. Docs and README rules: add a `tests/test_<topic>.py` that checks structure (counts, links, keys), not exact prose.
4. A new Keys row needs its `docs/switches/<key>.jpg` in the same PR (`tests/test_switches.py`).
5. Keep `.sh` and `.ps1` behaviour in step; PowerShell tests must skip when `pwsh` is missing.
