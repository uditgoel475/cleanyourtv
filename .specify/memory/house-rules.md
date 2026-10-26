---
generated-by: /ai-sdlc/memorize
generated-at: 2026-10-26
---

# House Rules Memory

Rules apply to production **and** test code unless a row says otherwise. Sources are repo docs:
`AGENTS.md` (A), `CONTRIBUTING.md` (C), `docs/DOS-AND-DONTS.md` (D), `.github/maintainer/MAINTAINER.md` (M),
`docs/workflows/launcher-switch.md` (W), and measured facts. Review history: no VCS tool was reachable and the
developer answered the corrections question with nothing on 2026-10-26, so reviewer-enforced rules come only from M and D,
which cite PR numbers.

## Rules

| ID | Rule | Don't | Do instead (approved alternative) | Applies to | Detect in diff | Source | Status |
|----|------|-------|-----------------------------------|------------|----------------|--------|--------|
| HR-01 | Disable, never uninstall | `pm uninstall`; `pm hide`/`pm block` without undo | `pm disable-user --user 0` via `tools/disable.sh`; undo with `tools/restore.sh` | lists, scripts, docs | `pm (uninstall\|hide\|block)` | A Hard rules; D Never; M Lists | documented |
| HR-02 | Never list Android core packages | `android`, `com.android.systemui`, `.shell`, `.phone`, `.settings`, `.tv.settings`, permissioncontroller, packageinstaller, ext services/shared, modulemetadata, `com.google.android.overlay.modules*`, `com.android.providers.*` (except calendar, contacts, userdictionary, blockednumber) | Leave them out of `debloat.txt` | `examples/**/debloat.txt` | `^\s*(android\|com\.android\.(systemui\|shell\|phone\|settings\|tv\.settings\|providers\.)\|com\.google\.android\.overlay\.modules)` | A Cleaning a TV; C section 1; M Lists (#884) | documented |
| HR-03 | Keep `CORE_RE`/`CORE_OK` identical in three places | Editing one copy | Change `tools/disable.sh`, `tools/disable.ps1`, `tools/recovery.py` together; run `tests/test_core_packages.py` | scripts | diff touches one of the three files' `CORE_` lines | `tools/disable.sh` comment above `CORE_RE` | documented |
| HR-04 | Launcher has no network | `android.permission.INTERNET`, analytics, telemetry, polling, background services, blur, video | Make it an add-on or a showcase fork | `launcher/` | `permission\.INTERNET` in `AndroidManifest.xml`; new `extends Service` in `launcher/` | A Hard rules; C section 2; M Scope | documented |
| HR-05 | No new permissions or grants in the launcher | New `uses-permission`, grants in install script | Keep to the existing set in `AndroidManifest.xml` | `launcher/` | `uses-permission` added | A Hard rules; D Launcher switches | documented |
| HR-06 | Switches are off by default | Changing a default for every TV | `config.xml` key, off by default, a theme or a TV's `config.json` turns it on | `launcher/` | new `<bool name=` default true; changed existing default | A Hard rules; W step 1; M Learned | documented |
| HR-07 | A new key lives in four places in one PR | Key only in code | `config.xml`, `Config.java`, `themes/README.md` Keys row, `docs/switches.md` row with `docs/switches/<key>.jpg` | launcher + docs | new key in `Config.java` without `themes/README.md` change | W steps 1-3; `tests/test_switches.py` | documented |
| HR-08 | Strings in every locale, or config-only | English-only Settings rows | Add to `values/strings.xml` and each of the 27 `values-*` folders | `launcher/res` | `strings.xml` changed without `values-*` | W step 4; D Launcher switches; M | documented |
| HR-09 | No new launcher dependencies | New `implementation` line | Discuss in an issue first | `launcher/app/build.gradle` | `^\+\s*implementation` | A Hard rules; C Pull requests | documented |
| HR-10 | Keep `minSdk` 26 | Lowering it | Fork plus `SHOWCASE.md` row | `launcher/app/build.gradle` | `minSdk` changed | A Hard rules | documented |
| HR-11 | APK stays under 589,824 B | Growth past the gate | Shorter strings, smaller code; build and check | `launcher/` | CI gate | A Before opening a PR; `.github/workflows/ci.yml` | documented |
| HR-12 | No prebuilt binaries | Committed APK, AAB, `.so`, zip | Build from source | repo | `\.(apk\|aab\|so\|zip)$` added | A Hard rules; D Never | documented |
| HR-13 | No secrets or identifiers | LAN IPs, Wi-Fi names, emails, serials, Android IDs, MACs, ADB keys | `<tv ip>` or `192.168.1.50` in docs | everywhere | `\b\d{1,3}(\.\d{1,3}){3}\b` other than `192.168.1.50`, `0.0.0.0`, `127.0.0.1` | A Hard rules; D Never | documented |
| HR-14 | Do not edit generated README tables or counts | Hand-editing TV rows, brand line, counts | Add `examples/<folder>/tv.json`; maintainer runs `tools/gen_tables.py` | `README.md`, `examples/README.md` | table row changes in a list PR | A Before opening a PR; C section 1; M Lists | documented |
| HR-15 | One topic per PR | A TV list inside a launcher PR | Separate PRs; a device's list plus its small fixes may be one | PRs | mixed `examples/` and `launcher/` | A; C; D Before you start | documented |
| HR-16 | No files nothing links to | Stray screenshots, reports, root files | Put screenshots in the PR description | repo | new unlinked file in `docs/` or root | A Before opening a PR; D Keeping a PR mergeable; M | documented |
| HR-17 | Do not edit `.github/workflows/` or the size gate in a normal PR | Workflow edits | Open an issue | `.github/workflows/` | any change there | A Hard rules; D Never; M Scope | documented |
| HR-18 | Do not commit agent or tool config | `.claude/`, `.codex/`, `.cursor/` hooks or similar (here also `.github/skills/`, `.github/instructions/`) | Keep local | repo | paths added | D Never (PR #286) | reviewer-enforced |
| HR-19 | Never start a factory or test package | `am start` of `com.cvte.tv.hkcfac`, `mediatek.factorymenu.ui` etc. | Disable by name only after reading `dumpsys package <name>` | scripts, agent runs | `factory` in `am start` | A Cleaning a TV | documented |
| HR-20 | Home guard | Disabling the stock launcher before the new one is Home; `FORCE_HOME=1`/`FORCE_SETUPWRAITH=1` as a shortcut | Use `tools/disable.sh` and `launcher/build-and-install.sh` | scripts | changes to guard logic | A Cleaning a TV; A Hard rules (maintainer owns changes) | documented |
| HR-21 | Protect user's data: back up before the first change | Changing a TV with no backup | Package list, disabled list, `getprop` and settings into `backup/` (git-ignored) | agent runs | n/a | A Cleaning a TV; `.gitignore` | documented |
| HR-22 | Heavy work off the main thread; bitmaps capped at 1920 px | Hashing or decoding on UI thread | Background thread (`HandlerThread`) | `launcher/` | decode/hash call in an Activity method | D Launcher switches (#378, #174) | reviewer-enforced |
| HR-23 | Do not swallow volume, mute or media keys, or log key presses | Consuming those keys; `Log` of key codes | Pass through | `launcher/` | `Log\.[dv]\(.*[Kk]ey` | D Never (#176); D Launcher switches (#257, #244) | reviewer-enforced |
| HR-24 | Public APIs only | Hidden fields via reflection | Public SDK calls | `launcher/` | `getDeclaredField\|setAccessible` | D Launcher switches (#1337, #407) | reviewer-enforced |
| HR-25 | Tests: no always-true tests, no pinning prose wording | `assertTrue(True)`, exact-sentence assertions | Check structure (counts, keys, links) or behaviour | `tests/`, `launcher/app/src/test` | `assertTrue\(True\)` | D Tools (#745, #858, #891) | reviewer-enforced |
| HR-26 | Test scripts with the real script and a fake transport | Mocking away the script under test | Fake `tv.py` driven by `FAKE_TV_STATE` | `tests/` | new test that patches the script itself | `tests/test_home_guard.py` (pattern in 23 files) | measured |
| HR-27 | Comments in English | Other languages | English | everywhere | non-ASCII-only comment text | D Keeping a PR mergeable (#402) | reviewer-enforced |
| HR-28 | Keep `AGENTS.md` and `ci.yml` commands in step | Changing the CI commands only | Update `AGENTS.md` too | `.github/workflows/ci.yml`, `AGENTS.md` | `ci.yml` run lines changed | `tests/test_agents_md.py` | measured |
| HR-29 | D-pad deterministic; verify on a real TV | Emulator only | `tools/tv.py --screenshot shots/after.png DPAD_DOWN` from Up/Down/Left/Right edges | `launcher/` | PR without "Tested on" | C section 2; A Before opening a PR | documented |
| HR-30 | Anything TV-specific goes in config | Device checks in code | `config.xml` or a theme | `launcher/` | `Build.MODEL`/`Build.DEVICE` comparisons added | C section 2 | documented |
| HR-31 | Tools go through `tools/tv.py` | Hardcoded activities, JAVA_HOME paths, fallback IP; a second install path | `tv.py`; `launcher/build-and-install.sh` is the one install path | `tools/` | `adb ` invocations in new scripts | D Tools (#437, #36, #1170) | reviewer-enforced |
| HR-32 | No Private DNS or AdGuard setup or install/restore scripts in a list folder | Extra scripts in `examples/<folder>/` | README points at `tools/disable.sh` and `tools/restore.sh` | `examples/` | `.sh`/`.ps1` added under `examples/` | A Hard rules; M Lists | documented |

## Legitimate Exceptions

| Rule | Exception | Source |
|------|-----------|--------|
| HR-02 | `com.android.providers.` calendar, contacts, userdictionary, blockednumber may be disabled | `tools/disable.sh` `CORE_OK` |
| HR-05 | `WRITE_SECURE_SETTINGS`, `ACCESS_FINE_LOCATION` and `yandex.tv.permission.ACCESS_TV_SOURCE` already in the manifest; granted by the install script | `launcher/app/src/main/AndroidManifest.xml` |
| HR-12 | `addons/*/gradle/wrapper/gradle-wrapper.jar` is required by add-ons CI | M Learned |
| HR-14 | A conflict only in `README.md`, `examples/README.md` or `SHOWCASE.md` needs no rebase | M Scope and taste |
| HR-20 | `FORCE_HOME`/`FORCE_SETUPWRAITH` only in the case the script message describes, with the person's agreement | A Cleaning a TV |
| HR-06 | A launcher bug fix that corrects wrong behaviour needs no flag | M Policy |

## Conflicts

| Topic | Version A (source) | Version B (source) | Resolution and who decided |
|-------|--------------------|--------------------|----------------------------|
| Stack rules | `.github/copilot-instructions.md`: Spring Boot constructor injection, DTOs, `ProblemDetail`, Testcontainers, one assertion per test, 85% branch coverage | `launcher/` is plain Java on Android; no Spring, no coverage tool (`launcher/app/build.gradle`) | The Spring rules do not apply. Repo-owned docs (A, C) decide. Resolved by this command, Phase 2 item 8 rule 1; confirm with the developer |
| Coverage | `.github/copilot-instructions.md`: 85% branch on new code | none configured (measured) | `[UNKNOWN - not configured]`; do not claim 85% |
| Agent config in the repo | D Never: do not commit `.claude/` or similar | `.claude/`, `.github/skills/`, `.github/instructions/`, `.github/copilot-instructions.md`, `.ai-sdlc-lock.json` exist untracked (`git status`: 5 untracked entries) | Keep local, do not commit |
| Scripts in list folders | A Hard rules and M Lists: no install or restore scripts in a list folder | 5 tracked scripts under `examples/` (anker-nebula-d2426, epson-ls800, tcl-43c645) | Legacy; do not add or copy |
| Secure-settings grants | D Launcher switches: no secure-settings grants, even behind a switch | Manifest declares `WRITE_SECURE_SETTINGS` for `ServiceRepair` | Existing exception; add no more |
| Spec location | `.github/copilot-instructions.md`: specs in `.specify/specs/<id>/` are the source of truth | A and M: unlinked files are dropped; `docs/workflows/` is the process | `.specify/` is local scratch; the repo's workflows win for what is committed |

## Artifact Policy

- SDD artifacts (`.specify/`) committed to the repo: no - keep local (D Never: agent/tool config; A and M: unlinked files are dropped). `.specify/` has 0 tracked files and is not in `.gitignore`; the developer decides whether to ignore it locally.
- Commit scope for this module: `area: what changed (#issue)`; areas in the last 300 subjects: examples 35, docs 33, SHOWCASE 27, tools 20, Launcher 13, README 11 (measured from `git log -300 --format=%s`; A Commit and PR style).

## Upstream Gaps

Reviewer-enforced or measured rules that `AGENTS.md` does not state:

- Java launcher tests are named as camelCase sentences with the issue cited in a comment (measured: 242 `@Test`, 0 snake_case).
- No coverage measurement exists; `.github/copilot-instructions.md` implies 85%.
- `CORE_RE`/`CORE_OK` live in three files (stated in a script comment, not in `AGENTS.md`).
- `.specify/` and agent config policy (stated only in D).
- 5 legacy scripts in `examples/` contradict the "no scripts in list folders" rule.
