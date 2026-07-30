# Contributing to Focus Lock

Thanks for your interest in improving Focus Lock! This guide captures the
conventions that keep the codebase healthy. The project is licensed under the
[GPL v3](./LICENSE); by contributing you agree your changes will be released
under the same license.

## Code of conduct

Participation in this project is governed by the
[Code of Conduct](./CODE_OF_CONDUCT.md). Please be excellent to one another.

## Prerequisites

- **JDK 21** (full JDK, must include `javac`) — required by AGP 9.
- **Android SDK** with platform **API 37**.
- Gradle is provided via the wrapper — you do **not** need a system Gradle.

If your default `java` is a JRE, set `org.gradle.java.home` in
`$GRADLE_USER_HOME/gradle.properties` to a full JDK 21.

## Getting the code

```bash
git clone <your-repo-url> focus-lock
cd focus-lock
./gradlew :app:assembleDebug   # sanity check
```

## Development workflow

1. Create a branch off `main`:
   `git switch -c feat/<short-description>` (or `fix/`, `chore/`, `docs/`).
2. Make your changes in small, reviewable commits.
3. Keep tests green: `./gradlew :app:testDebugUnitTest`.
4. Run lint: `./gradlew :app:lintDebug`.
5. Open a pull request against `main` and fill in the PR template.

## Code style

- Follow the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
  (`kotlin.code.style=official` is set in `gradle.properties`).
- **Indentation: 4 spaces.** No tabs.
- Max line length: **140 characters**.
- One top‑level declaration per file; file names match the primary class.
- Prefer `val` over `var`; prefer expression bodies for one‑liners.
- Use `data class` for pure data holders; keep `equals`/`hashCode`/`toString` in
  mind when extending them.
- **Do not commit commented‑out code** or `println` debugging.
- Write **KDoc** on public APIs; explain *why*, not *what*.

### Compose

- Stateful composables stay small; hoist state to the ViewModel/`remember`.
- Prefer `collectAsState()` on the ViewModel’s `StateFlow`s.
- Pass `Modifier` as the first optional parameter of public composables.
- Extract reusable UI into `@Composable` functions under `view/screens`.

### Dependency injection

- Use constructor injection (`@Inject constructor`); never call `Hilt`/component
  APIs directly.
- Provide framework‑bound types (`Context`, `Vibrator`, …) in Hilt modules under
  `di/`, not at call sites.
- Scope bindings deliberately: `@Singleton` only when the object must be shared.

## Managing dependencies

All versions live in [`gradle/libs.versions.toml`](./gradle/libs.versions.toml).
**Do not hardcode versions** in module build scripts — add a version alias and
reference it. Prefer stable releases; pin alphas/betas only with a reason.

## Commit conventions

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

[optional body]

[optional footer(s)]
```

- **type**: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`,
  `build`, `ci`, `chore`, `revert`.
- **scope** (optional): a module/package, e.g. `timer`, `overlay`, `ui`.
- Subject in the imperative, lowercase, ≤ 72 chars, no trailing period.
- Reference issues in the footer: `Closes #123`.

Examples:

```
feat(timer): anchor countdown to SystemClock elapsedRealtime
fix(overlay): re-attach window when accessibility detects a switch
docs(readme): document GPLv3 license and build setup
```

## Testing expectations

- **Every bug fix and new feature must ship with a test** that would fail
  without the change.
- Pure logic (engines, mappers, reducers) → JVM unit tests under `app/src/test`.
- Anything needing Android framework → instrumented tests under
  `app/src/androidTest`.
- Name tests `methodName_condition_expectedResult`.
- Unit tests run with `isReturnDefaultValues = true`; if your code calls Android
  framework APIs that must return realistic values, prefer injecting an
  abstraction rather than relying on default stubs.

Run everything before pushing:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest   # if you add instrumented tests
```

## Pull‑request checklist

Before requesting review, confirm:

- [ ] Branch is up to date with `main`.
- [ ] `./gradlew :app:assembleDebug` succeeds.
- [ ] `./gradlew :app:testDebugUnitTest` succeeds.
- [ ] `./gradlew :app:lintDebug` introduces **no new** warnings/errors.
- [ ] New/changed behavior is covered by tests.
- [ ] Public API has KDoc.
- [ ] No secrets, keys, or `local.properties` content committed.
- [ ] Commits follow Conventional Commits and are squashed if noisy.

## Release signing

`assembleRelease` is **unsigned** by default. To produce an installable release,
define a signing config without committing secrets:

1. Create a keystore locally (`keytool -genkey ...`).
2. Add credentials to a **git‑ignored** `keystore.properties` at the repo root:

   ```properties
   storeFile=../your-release.keystore
   storePassword=********
   keyAlias=release
   keyPassword=********
   ```

3. (Contributors who maintain releases only) wire it into `app/build.gradle.kts`
   via a guarded `signingConfigs` block that reads `keystore.properties` when
   present and is absent otherwise — so CI and other contributors still build.

Add `keystore.properties` and `*.keystore` to `.gitignore`.

## Reporting issues

- Search existing issues before opening a new one.
- Include: device + Android version, app version, steps to reproduce, expected
  vs. actual behavior, and logcat output if available.
- Security issues: see the contact in the README — **do not** open a public issue.

Thank you for contributing! 🎉
