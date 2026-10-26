---
generated-by: /ai-sdlc/memorize
generated-at: 2026-10-26
---

# Dependencies Memory

## Runtime Dependencies

| Package | Version | Category | Why Used |
|---------|---------|----------|---------|
| `androidx.recyclerview:recyclerview` | 1.3.2 | UI | app grid and dock (`launcher/app/build.gradle`) |
| `androidx.dynamicanimation:dynamicanimation` | 1.0.0 | UI | spring animation of the glass highlight |
| `junit:junit` | 4.13.2 | test (not in APK) | JVM unit tests |
| `adb-shell` | ==0.4.4 | ADB | `tools/tv.py` without an `adb` binary (`tools/requirements.txt`) |
| `rsa` | ==4.9.1 | auth | ADB key signing |
| `cryptography` | >=46.0.7 | auth | ADB key handling |
| `perfetto` | ==0.58.2 | perf (optional, `tools/requirements-perf.txt`) | `trace_report.py`, `flamechart.py`, `screendiff.py` |
| `pillow` | ==11.3.0 | docs (optional, `tools/requirements-perf.txt`) | README card scripts `readme_stats.py`, `readme_featured.py`, `readme_card.py` |
| Android SDK | compileSdk 34, minSdk 26, targetSdk 34 | platform | `launcher/app/build.gradle` |
| JDK | 17 | build | `sourceCompatibility`/`targetCompatibility` 17; CI uses Temurin 17 |

`tools/requirements-perf.txt` is optional: `docs/DOS-AND-DONTS.md` keeps optional tools out of `tools/requirements.txt`.

## External Integrations

| System | How Connected | Config / Secret |
|--------|--------------|----------------|
| The TV | ADB over TCP, plaintext port 5555 | `TV_HOST` |
| Wi-Fi name pill | `WifiManager`, `ACCESS_FINE_LOCATION` granted with `pm grant` by the install script | manifest |
| Remote helpers | Accessibility services enabled with `settings put secure ...` | `WRITE_SECURE_SETTINGS` granted by install script |
| Yandex TV OS source player | permission `yandex.tv.permission.ACCESS_TV_SOURCE` | manifest |
| Module cards | package visibility `<queries>` for category `com.gathin.tvhome.category.DOCK_CARD` | `docs/specs/card.md` |
| GitHub Actions | `ci.yml`, `addons.yml`, `maintainer-bot.yml` | repo settings |
| Network from the launcher | none: no `INTERNET` permission | n/a |

## Environment Variables Required

| Variable | Purpose | Required? |
|----------|---------|----------|
| `TV_HOST` | TV IP or IP:port; `tv.py` exits without it (not for `--help`) | yes, for any TV command |
| `FORCE_HOME` | override the Home guard in `disable.sh`; only in the situation the script message describes, with the person's agreement | no |
| `FORCE_SETUPWRAITH` | disable setupwraith despite owning saved Wi-Fi (e.g. Ethernet) | no |
| `STOCK_LAUNCHER` | stock launcher package for `build-and-install.sh`; `none` keeps it enabled | no |
| `CLEANYOURTV_STRICT` | when set, tests guarded by `skipUnless(os.environ.get("CLEANYOURTV_STRICT"))` run (e.g. committed tables sorted, `tests/test_gen_tables.py`); CI sets it on push to main only | no |
| `FAKE_TV_STATE` | path to JSON state for fake `tv.py` in tests | tests only |
| Gradle property `launcherApplicationId` | alternate application id for firmware with a Home package allowlist | no |

## Known Constraints

- `minSdk` stays 26; lowering it is a fork for `SHOWCASE.md` (`AGENTS.md` Hard rules, `docs/DOS-AND-DONTS.md`).
- No new launcher dependencies without an issue first (`AGENTS.md`).
- APK size gate 589,824 B; targets TVs with 1 GB of RAM (`.github/workflows/ci.yml`, `AGENTS.md`).
- Stock `adb` is blocked on recent macOS, hence `tools/tv.py` and `tools/b64push.py` (`CONTRIBUTING.md` section 3).
- Release APK is signed with the debug key so it installs over a debug build (`launcher/app/build.gradle`).
- Kotlin metadata is excluded from the APK (`packagingOptions` in `launcher/app/build.gradle`).
- Windows consoles with old code pages: `tv.py` reconfigures stdout/stderr with `errors="replace"`.
- Add-ons carry `gradle/wrapper/gradle-wrapper.jar` because the add-ons CI needs it; this is not a banned prebuilt binary (`.github/maintainer/MAINTAINER.md`).
