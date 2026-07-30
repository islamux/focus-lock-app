# AGENTS.md

Instructions for AI coding agents (opencode, Claude Code, Codex, etc.) working in
this repository. Follow these unless a human gives a direct, conflicting
instruction.

## Project

Focus Lock — an Android app that enforces focus sessions via a persistent
system overlay, a foreground service, an accessibility service, and a
hardware‑clock countdown. Kotlin + Jetpack Compose + Hilt + Room. Licensed under
**GNU GPLv3**.

## Environment (read first)

- **JDK 21** is required to run Gradle (AGP 9). It must be a **full JDK**
  (with `javac`), not a JRE. If `javac` is missing, set
  `org.gradle.java.home` in `$GRADLE_USER_HOME/gradle.properties` to a full JDK 21.
- Use the **Gradle wrapper** (`./gradlew`); never invoke a system `gradle`.
- `compileSdk = 37`, `targetSdk = 34` (deliberately), `minSdk = 26`,
  Java/Kotlin target = **17**.

## Common commands

```bash
./gradlew :app:assembleDebug          # Build debug APK
./gradlew :app:assembleRelease        # Minified unsigned release APK
./gradlew :app:testDebugUnitTest      # JVM unit tests
./gradlew :app:lintDebug              # Lint (HTML+SARIF reports under app/build/reports)
./gradlew :app:dependencies           # Inspect resolved versions
```

Debug APK → `app/build/outputs/apk/debug/app-debug.apk`.
Release APK → `app/build/outputs/apk/release/app-release-unsigned.apk`.

After any non‑trivial change, run `assembleDebug` + `testDebugUnitTest` and ensure
both are green before claiming completion.

## Critical conventions & gotchas

- **AGP 9 ships built‑in Kotlin.** Do **not** add the
  `org.jetbrains.kotlin.android` plugin — applying it is a hard error. Kotlin is
  provided by `com.android.application`. The Compose compiler plugin
  (`org.jetbrains.kotlin.plugin.compose`) is still applied explicitly.
- **Versions live only in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).**
  Never hardcode a dependency/plugin version in `*.gradle.kts`. Add a `[versions]`
  alias and reference it. KSP is independently versioned (e.g. `2.3.10`), not
  tied to the Kotlin version.
- **Kotlin/Java target is 17.** Set it via `compileOptions` and
  `kotlin { compilerOptions { jvmTarget = JVM_17 } }` — don’t reintroduce the
  deprecated top‑level `kotlinOptions { ... }` block.
- **Window overlay flags** (`FLAG_FULLSCREEN`, `FLAG_SHOW_WHEN_LOCKED`,
  `FLAG_DISMISS_KEYGUARD`) in `overlay/OverlayManager.kt` have no Activity‑scoped
  modern equivalent — they are intentionally under `@Suppress("DEPRECATION")`.
  Don’t “fix” them by deleting them.
- **`CountdownEngine` must stay anchored to `SystemClock.elapsedRealtime()`** —
  that is the tamper‑resistance guarantee. Do not switch to wall‑clock time.
- **Unit tests run with `isReturnDefaultValues = true`** (so framework stubs like
  `SystemClock` return defaults). Prefer constructor‑injected abstractions for
  testability over relying on stub defaults.
- **No comments in code unless requested** or the “why” is genuinely non‑obvious.
  Do not leave commented‑out code or `println` debugging.
- Keep new dependencies minimal and justify them. Prefer AndroidX/first‑party.

## Architecture pointers

- Layered single module: `view`/`viewmodel` → `services` →
  `data` (Room) + `timer` + `overlay`. See [`ARCHITECTURE.md`](ARCHITECTURE.md).
- `MainViewModel` is the single source of UI state; expose `StateFlow` only.
- DI is Hilt — use `@Inject constructor`; provide framework types in `di/`.
- Overlay Compose tree runs outside any Activity; `OverlayManager` supplies its
  own `LifecycleOwner`/`ViewModelStoreOwner`/`SavedStateRegistryOwner`.

## When you finish a task

1. `./gradlew :app:assembleDebug :app:testDebugUnitTest` → both green.
2. `./gradlew :app:lintDebug` → no **new** warnings introduced.
3. Do not commit unless explicitly asked. If asked to commit, write a Conventional
   Commit message (see [`CONTRIBUTING.md`](CONTRIBUTING.md)) and never commit
   secrets, `local.properties`, or a keystore.
4. This is a GPLv3 project — keep new source files consistent with that (the
   repo has a `LICENSE` and per‑file GPL headers).
