# AGENTS.md

Android TV home launcher. Single-module Gradle project (`app/`) with an embedded Rust JNI library for search ranking. UI is programmatic platform Views — no Compose, no XML layouts, no Leanback, no RecyclerView. Single activity (`MainActivity`), single dependency (`androidx.activity`).

## Build prerequisites

- JDK 17, Android SDK Platform 36, NDK **27.2.12479018**
- Rust with targets: `aarch64-linux-android`, `armv7-linux-androideabi`, `x86_64-linux-android`
- `ANDROID_NDK_HOME` must point to the exact NDK version (or it auto-resolves from `ANDROID_HOME/ndk/27.2.12479018`)

## Commands

```powershell
# Full CI check (this is what CI runs — always run all three together)
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug

# Rust-only tests (runs on host, no Android target needed)
cargo test --manifest-path app/src/main/rust/Cargo.toml

# Docs site (separate from the app build)
cd site && npm ci && npm run check && npm run build
```

Gradle calls `cargo build --release --locked` for each ABI automatically before merging JNI libs. You do **not** need to run Cargo separately for an APK build.

## Architecture

```
app/src/main/kotlin/com/bluelabs/bluelauncher/
├── MainActivity.kt              # Single activity, entry point
├── data/                         # AppRepository, LauncherPreferences, AppCategory, WallpaperStore
├── nativeengine/NativeEngine.kt  # JNI bridge to Rust scorer; has Kotlin fallback
├── ui/                           # TvUi, Palette, LauncherViews, components/, dialogs/
└── viewmodel/LauncherViewModel.kt

app/src/main/rust/src/lib.rs      # Search scoring + JNI exports (cdylib → libblue_launcher.so)
```

- `NativeEngine.rankApps` is the JNI entry point. If the native lib fails to load, Kotlin's `matchScore` takes over — the Kotlin fallback omits the subsequence scorer.
- R8 keeps `NativeEngine` and native methods via `proguard-rules.pro`.
- The Rust ↔ Kotlin protocol is tab-separated records over newlines. Labels containing `\t` or `\n` are sanitized on the Kotlin side.

## Testing

- **JVM unit tests** (`testDebugUnitTest`): preference merging, favorites ordering, category filters, `TvUi.glyphRuns`. Pure JVM, no Robolectric.
- **Rust tests** (`cargo test`): search scoring, ranking, Unicode safety. Host-only.
- **Lint** (`lintDebug`): `abortOnError = true` — errors fail the build, warnings do not.
- No instrumented/emulator tests exist.

## Conventions

- **No new dependencies** without discussion. The project ships with a single AndroidX artifact.
- **No XML layouts or Compose.** All UI is built programmatically in code.
- **User-facing strings** go in `app/src/main/res/values/strings.xml`.
- Kotlin style: standard Android/Kotlin 4-space indent. Rust: `rustfmt`.
- Prefer pure, testable helpers — add JVM tests for any preference/ordering logic.

## Gotchas

- Gradle config cache is **disabled** (`gradle.properties`); the custom Cargo Exec tasks don't use serializable providers yet.
- `--locked` is passed to Cargo — changes to Rust dependencies require updating `app/src/main/rust/Cargo.lock` explicitly.
- ARM64 and x86_64 `.so` files are linked with 16 KB page alignment for Android 15+. 32-bit ARM stays at 4 KB. This is handled automatically by the Gradle build script.
- `unitTests.isReturnDefaultValues = true` — Android framework stubs return defaults instead of throwing, which can mask incorrect calls.

## Docs site (`site/`)

Standalone Astro static site, deployed via GitHub Pages on pushes to `main` that touch `site/**`. Not part of the Gradle build. Uses Node 24, `npm ci`, `astro check`, `astro build`.
