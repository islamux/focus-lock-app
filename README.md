# Focus Lock

> An Android focus‑productivity app that enforces uninterrupted work sessions by
> drawing a persistent, full‑screen overlay above every other app for a chosen
> duration. Built with Jetpack Compose, Hilt, Room, and a tamper‑resistant
> countdown engine.

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](./LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-purple.svg)](https://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/Gradle-9.6.1-02303A?logo=gradle&logoColor=white)](https://gradle.org)
[![Android](https://img.shields.io/badge/Android-Compile%20API%2037-3DDC84?logo=android&logoColor=white)](https://developer.android.com)

---

## Table of contents

- [Overview](#overview)
- [Features](#features)
- [Tech stack](#tech-stack)
- [Requirements](#requirements)
- [Getting started](#getting-started)
- [Building](#building)
- [Testing](#testing)
- [Linting](#linting)
- [Project structure](#project-structure)
- [Architecture](#architecture)
- [Permissions](#permissions)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

Focus Lock helps you commit to deep‑work sessions by literally locking the device
to a single focus task. When a session starts, a foreground service raises a
`TYPE_APPLICATION_OVERLAY` window that covers the screen and stays on top while a
hardware‑clock‑backed countdown runs. An accessibility service watches for app
switches and re‑attaches the overlay if the user attempts to escape. Sessions and
cumulative focus time are persisted in a Room database.

## Features

- ⏱ **Hardware‑clock countdown** — uses `SystemClock.elapsedRealtime()`, so the
  timer is immune to device clock changes.
- 🛡 **Persistent overlay** — full‑screen `SYSTEM_ALERT_WINDOW` overlay that
  survives app‑switch attempts via an accessibility service.
- 🔔 **Foreground service** with a `specialUse` type and an ongoing notification.
- 🧠 **Clean, layered architecture** — presentation / domain / data, wired with
  Hilt dependency injection.
- 💾 **Room persistence** — session history, total focus time, completion counts.
- 🎨 **Material 3 + Jetpack Compose** — neon “locked‑in” dark UI.
- 🔄 **Boot resilience** — a `BOOT_COMPLETED` receiver to reset session state.

## Tech stack

| Area | Technology |
| --- | --- |
| Language | Kotlin 2.4.10 (built‑in Kotlin via AGP 9) |
| UI | Jetpack Compose (BOM `2026.06.01`), Material 3 |
| DI | Hilt `2.60.1` |
| Persistence | Room `2.8.4` |
| Async | Kotlinx Coroutines `1.11.0` + Flow |
| Navigation | Navigation‑Compose `2.9.8` |
| Annotation processing | KSP `2.3.10` |
| Build | Gradle `9.6.1`, Android Gradle Plugin `9.3.1` |

See [`gradle/libs.versions.toml`](./gradle/libs.versions.toml) for the canonical version catalog.

## Requirements

- **JDK 21** (required to run Gradle under AGP 9) — must be a full JDK (include
  `javac`), not a JRE.
- **Android SDK** with platform **API 37** installed.
- Minimum runtime: **Android 8.0 (API 26)** · Target: **API 34** · Compile: **API 37**.

> If your system `java` is a JRE‑only install (no `javac`), point Gradle at a full
> JDK 21 by setting `org.gradle.java.home` in `$GRADLE_USER_HOME/gradle.properties`.

## Getting started

```bash
# 1. Clone
git clone <your-repo-url> focus-lock
cd focus-lock

# 2. Build the debug APK
./gradlew :app:assembleDebug

# 3. Install on a connected device/emulator
./gradlew :app:installDebug
```

The debug APK is emitted at `app/build/outputs/apk/debug/app-debug.apk`.

Or open the project in **Android Studio** (Narwhal/Weasel or newer), select a
device, and press ▶ Run.

## Building

```bash
./gradlew :app:assembleDebug      # Debug build
./gradlew :app:assembleRelease    # Minified + resource-shrunk release (unsigned)
```

The release variant enables R8 (`isMinifyEnabled`) and resource shrinking. It is
**unsigned** by default — to produce an installable release, configure a signing
keystore (see [Contributing](./CONTRIBUTING.md#release-signing)).

## Testing

```bash
./gradlew :app:testDebugUnitTest        # JVM unit tests
./gradlew :app:connectedDebugAndroidTest # Instrumented tests (needs a device)
```

Unit tests run with `testOptions.unitTests.isReturnDefaultValues = true` so that
Android framework stubs (e.g. `SystemClock`) return defaults instead of throwing
in the plain JVM environment.

## Linting

```bash
./gradlew :app:lintDebug          # Lint the debug variant
./gradlew :app:lintVitalRelease   # Vital lint gate run during release builds
```

Reports are written to `app/build/reports/lint-results-debug.{html,sarif}`.

## Project structure

```
FocusLock/
├── app/
│   ├── build.gradle.kts                # App module config
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/focuslock/app/
│       │   │   ├── FocusLockApp.kt            # @HiltAndroidApp Application
│       │   │   ├── data/                       # Room DB, DAO, entities
│       │   │   ├── di/                         # Hilt modules (App, Database)
│       │   │   ├── domain/                     # Domain models
│       │   │   ├── overlay/                    # WindowManager overlay host
│       │   │   ├── presentation/               # Compose UI, nav, ViewModel, theme
│       │   │   ├── services/                   # Foreground svc, accessibility, boot receiver
│       │   │   └── timer/                      # CountdownEngine (hardware clock)
│       │   └── res/                            # Strings, themes, icons, XML configs
│       └── test/                               # Unit tests
├── gradle/
│   ├── libs.versions.toml               # Centralized version catalog
│   └── wrapper/                          # Gradle wrapper jar + properties
├── build.gradle.kts                     # Root build script
├── settings.gradle.kts                  # Module + repository settings
└── LICENSE                              # GNU GPLv3
```

## Architecture

Focus Lock follows a layered, single‑module clean architecture. See
[`ARCHITECTURE.md`](./ARCHITECTURE.md) for the full design notes.

- **Presentation** — Jetpack Compose screens + a `MainViewModel` exposing
  `StateFlow` UI state; navigation via a `NavHost`.
- **Domain** — plain Kotlin models (`FocusSession`).
- **Data** — Room database (`FocusDatabase`) with `FocusSessionDao` and
  `FocusSessionEntity`; reactive `Flow`-based queries.
- **Services** — `FocusForegroundService` (session lifecycle + notification),
  `FocusAccessibilityService` (anti‑escape), `BootCompletedReceiver`.
- **Overlay** — `OverlayManager` hosts a Compose tree inside a window overlay by
  wiring its own `LifecycleOwner`/`ViewModelStoreOwner`/`SavedStateRegistryOwner`.
- **Timer** — `CountdownEngine` ticks on `Dispatchers.Default` anchored to
  `SystemClock.elapsedRealtime()`.

Dependency injection is centralized in Hilt modules under `di/`.

## Permissions

| Permission | Why |
| --- | --- |
| `SYSTEM_ALERT_WINDOW` | Draw the focus overlay above other apps |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` | Run the session service |
| `POST_NOTIFICATIONS` | Show the ongoing‑session notification (Android 13+) |
| `RECEIVE_BOOT_COMPLETED` | Reset state after a reboot |
| `VIBRATE` | Haptic feedback on session completion |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Keep the timer alive |
| `BIND_ACCESSIBILITY_SERVICE` | Re‑attach the overlay on app switches |

## Roadmap

- [ ] Allowlist of permitted apps during a session
- [ ] Widget to start a session from the home screen
- [ ] Session statistics charts
- [ ] Optional strict mode that blocks notification shade pulls
- [ ] Backup/export of session history

## Contributing

Contributions are welcome! Please read [`CONTRIBUTING.md`](./CONTRIBUTING.md)
for code style, commit conventions, testing expectations, and the pull‑request
checklist. By participating you agree to abide by the
[Code of Conduct](./CODE_OF_CONDUCT.md).

## License

Copyright © 2026 islamux. Licensed under the
**GNU General Public License v3.0** — see [`LICENSE`](./LICENSE) for the full
text. Software distributed under the GPL is distributed on an *"AS IS"* basis,
**without warranties or conditions of any kind**, either express or implied.
