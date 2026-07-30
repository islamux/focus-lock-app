# Architecture

This document describes how Focus Lock is structured and how its moving parts
cooperate. It is intended for contributors who want a mental model before
changing code.

## Design principles

1. **Single source of truth per concern.** UI state lives in one
   `MainViewModel`; persistence lives in Room; time lives in `CountdownEngine`.
2. **Unidirectional data flow.** The UI observes `StateFlow`; user intents are
   funnels into the ViewModel, which calls services / repositories.
3. **Decoupled time.** The countdown is anchored to a hardware clock so device
   clock tampering cannot shorten a session.
4. **Defensive overlay.** The overlay is owned by a foreground service and
   re‑attached by an accessibility service, so no single entry point can be used
   to escape a session.

## High‑level layers

```
 ┌──────────────────────────────────────────────────────────┐
 │ Presentation (Jetpack Compose)                            │
 │   HomeScreen · FocusOverlayContent · FocusLockTheme       │
 │   FocusLockNavGraph · MainViewModel                       │
 └───────────────▲──────────────────────────┬───────────────┘
                 │ StateFlow (observe)        │ intents (start/stop)
 ┌───────────────┴──────────────────────────▼───────────────┐
 │ Application services                                      │
 │   FocusForegroundService · FocusAccessibilityService     │
 │   BootCompletedReceiver                                   │
 └───────────────▲──────────────────────────▲───────────────┘
                 │ start/observe              │ DI (@Inject)
 ┌───────────────┴────────────┐  ┌───────────┴──────────────┐
 │ CountdownEngine (timer)    │  │ OverlayManager (window)  │
 └───────────────▲────────────┘  └───────────────▲──────────┘
                 │                                 │ queries
 ┌───────────────┴─────────────────────────────────┴────────┐
 │ Data (Room) — FocusDatabase · FocusSessionDao            │
 └──────────────────────────────────────────────────────────┘
```

## Modules / packages

| Package | Responsibility |
| --- | --- |
| `com.focuslock.app` | `FocusLockApp` — `@HiltAndroidApp`, creates the notification channel. |
| `view` | `MainActivity`, `FocusLockNavGraph`. |
| `view.screens` | `HomeScreen` (session setup + stats) and `FocusOverlayContent` (the locked‑in overlay UI). |
| `view.theme` | Compose `FocusLockTheme` (Material 3 dark color scheme + tokens). |
| `viewmodel` | `MainViewModel` — exposes `StateFlow` UI state and intent handlers. |
| `domain.model` | Plain Kotlin domain models (`FocusSession`). |
| `database` | Room `FocusDatabase`, `FocusSessionDao`, `FocusSessionEntity`. |
| `di` | Hilt modules (`AppModule`, `DatabaseModule`). |
| `services` | Foreground service, accessibility service, boot receiver. |
| `overlay` | `OverlayManager` — hosts Compose inside a window overlay. |
| `timer` | `CountdownEngine` + `TimerState` sealed hierarchy. |

## Key flows

### Starting a session

1. `HomeScreen` collects user input and calls `MainViewModel.startFocusSession(...)`.
2. The ViewModel computes total seconds and starts `FocusForegroundService` with
   an `ACTION_START_SESSION` intent carrying the duration/tag/note.
3. The service calls `startForeground(...)` with the `specialUse` type and the
   ongoing notification, persists a `FocusSessionEntity` to Room, asks
   `OverlayManager.showOverlay(...)` to raise the overlay, and starts
   `CountdownEngine`.
4. `CountdownEngine` emits `TimerState.Running` on `Dispatchers.Default`; the
   service observes it, pushes the remaining seconds into a `StateFlow` the
   overlay renders, and refreshes the notification each tick.

### Completing a session

1. `CountdownEngine` reaches zero → emits `TimerState.Completed` and invokes the
   finish callback.
2. The service vibrates a waveform, marks the Room row `isCompleted = true`,
   stops the timer, removes the overlay, stops foreground, and (on completion)
   launches `MainActivity` to surface a completion state.

### Anti‑escape

`FocusAccessibilityService` monitors window‑state changes; when it detects the
foreground leaving the focus task while a session is active, it asks
`OverlayManager` to re‑attach the overlay. The overlay window uses flags that
keep it visible over the lock screen.

## State management

- All reactive state is exposed as **`StateFlow`** (hot, single‑source).
- Compose collects it with `collectAsState()`.
- The timer state machine is a sealed class:

  ```
  TimerState
   ├── Idle
   ├── Running(remainingSeconds, totalSeconds, progress)
   └── Completed
  ```

## Dependency injection (Hilt)

- `FocusLockApp` is `@HiltAndroidApp` → generates the application component.
- `@AndroidEntryPoint` on `MainActivity` and the foreground service enables
  member injection.
- `DatabaseModule` provides the `FocusDatabase` (`@Singleton`,
  `fallbackToDestructiveMigration(dropAllTables = true)`) and the DAO.
- `AppModule` provides framework‑bound collaborators (e.g. `Vibrator`,
  `CountdownEngine`, `OverlayManager`) — all `@Singleton` where appropriate.
- `MainViewModel` is `@HiltViewModel` with `@Inject constructor`.

## Persistence (Room)

- `FocusDatabase` holds `FocusSessionEntity` (duration, remaining, start time,
  tag, note, completed flag).
- `FocusSessionDao` exposes reactive `Flow` queries:
  `getAllSessions()`, `getTotalFocusTimeSeconds()`, `getCompletedSessionCount()`
  plus `insertSession`, `updateSession`, `getSessionById`.
- The ViewModel turns these `Flow`s into `StateFlow`s via `stateIn(...)` with
  `SharingStarted.WhileSubscribed(5000)`.

## Overlay rendering

`OverlayManager` adds a `ComposeView` to the `WindowManager` with
`TYPE_APPLICATION_OVERLAY`. Because the view lives outside any Activity, the
manager supplies its own `OverlayLifecycleOwner` implementing
`ViewModelStoreOwner` + `SavedStateRegistryOwner` so Compose has a valid tree.
The window flags (`FLAG_LAYOUT_NO_LIMITS`, lock‑screen visibility, etc.) have no
Activity‑scoped modern equivalent and are therefore applied under a scoped
`@Suppress("DEPRECATION")`.

## Time & tamper resistance

`CountdownEngine` computes a target realtime anchor:

```
targetRealtimeMs = SystemClock.elapsedRealtime() + durationSeconds * 1000
```

Each tick recomputes remaining time from `SystemClock.elapsedRealtime()`, so
changing the wall clock does **not** shorten the session. The tick runs on
`Dispatchers.Default` with a 500 ms cadence and updates a `MutableStateFlow`.

## Build configuration notes

- AGP 9 provides **built‑in Kotlin** — the project does **not** apply
  `org.jetbrains.kotlin.android`.
- `compileSdk = 37`, `targetSdk = 34` (deliberately kept below compile to avoid
  opting into new runtime behavior; bump separately when ready).
- Java/Kotlin bytecode target is **17** via `compileOptions` and
  `kotlin { compilerOptions { jvmTarget = JVM_17 } }`.
- All third‑party versions live in `gradle/libs.versions.toml` — bump them there.
