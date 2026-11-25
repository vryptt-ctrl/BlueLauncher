# Contributing to BlueLauncher

Thanks for your interest in improving BlueLauncher! This document covers the essentials for getting a development environment running and submitting changes.

## Development requirements

- Android Studio (or command-line SDK) with **JDK 17**
- Android SDK Platform **36** + Build Tools
- Android NDK **27.2.12479018**
- Rust toolchain with targets: `aarch64-linux-android`, `armv7-linux-androideabi`, `x86_64-linux-android`

## Getting started

```sh
git clone https://github.com/vryptt-ctrl/BlueLauncher.git
cd BlueLauncher
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
```

Open the folder in Android Studio, or build from the command line:

```sh
# Windows PowerShell
$env:ANDROID_NDK_HOME = "$env:ANDROID_HOME/ndk/27.2.12479018"
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

```sh
# macOS / Linux
export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/27.2.12479018"
./gradlew assembleDebug testDebugUnitTest lintDebug
```

The Gradle build invokes Cargo for all three ABIs automatically; native sources live in `app/src/main/rust`.

## Testing

- JVM unit tests: `./gradlew testDebugUnitTest` (pure preference/category logic, no Robolectric)
- Rust scorer tests: `cargo test --manifest-path app/src/main/rust/Cargo.toml` (host target)
- Lint: `./gradlew lintDebug` — the build fails on errors; advisories are tracked but non-blocking

## Before opening a pull request

1. Rebase on the latest `main`.
2. Make sure all three checks above pass locally.
3. Keep changes minimal and consistent with the project's goals: **fast cold start, low memory, no unnecessary background work, minimal dependencies**.
4. New user-facing strings go in `app/src/main/res/values/strings.xml`; layout code stays programmatic (no XML layouts, no Compose).
5. Describe **what** and **why** in the PR description; link any related issues.

## Code style

- Kotlin follows the standard Android/Kotlin style (4-space indent); Rust is formatted with `rustfmt`.
- Prefer pure, testable helpers for preference/ordering logic — add JVM tests for them.
- Avoid adding dependencies without discussing the trade-off first; the project deliberately ships with a single AndroidX artifact.

## Reporting issues

Include: device/TV model, Android version, steps to reproduce, expected vs actual behavior, and relevant `adb logcat` output. Feature requests are welcome but will be weighed against the project's size-and-speed priorities.
