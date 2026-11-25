/** Install, build and architecture copy. Commands are copied from README.md. */

export type Step = {
  title: string;
  body: string;
  code?: string;
  lang?: string;
  note?: string;
};

export const INSTALL_STEPS: Step[] = [
  {
    title: 'Get the APK',
    body: 'Download the latest release build from GitHub. Release builds are R8-shrunk and unsigned — the repository ships no production signing key, so install through adb or sideload as you prefer.',
    note: 'Android 6.0 (API 23) and newer. ARM64, ARMv7 and x86_64 are packaged.',
  },
  {
    title: 'Install over adb',
    body: 'Connect to the TV over the network or USB, then install the APK.',
    code: 'adb install -r app-debug.apk',
    lang: 'shell',
  },
  {
    title: 'Make it the home screen',
    body: 'Set BlueLauncher as the default home activity, or pick it from the TV home-app chooser the next time Android asks.',
    code: 'adb shell cmd package set-home-activity com.bluelabs.bluelauncher/.MainActivity',
    lang: 'shell',
  },
];

export const INSTALL_TROUBLESHOOTING = [
  {
    title: 'The installer rejects the APK',
    body: 'Release builds are unsigned. Either build a signed variant yourself with a private signing config, or install the debug APK, which is signed with the standard debug key.',
  },
  {
    title: 'The TV does not list BlueLauncher as a home app',
    body: 'BlueLauncher registers both HOME and LEANBACK_LAUNCHER. Some vendor launchers cache the home chooser — clear that app’s data or use the adb command above.',
  },
  {
    title: 'No apps appear in the grid',
    body: 'On Android 11+ only apps declared through launcher intent queries are visible by design. Apps without a TV or launcher icon will not show up.',
  },
  {
    title: 'Native search reports “Kotlin fallback”',
    body: 'The packaged ABIs are arm64-v8a, armeabi-v7a and x86_64. Other ABIs fall back to the Kotlin scorer, which ranks identically but slower.',
  },
];

export const BUILD_PREREQS = [
  { label: 'JDK', value: '17', note: 'Android Studio’s bundled JDK works.' },
  {
    label: 'Android SDK',
    value: 'Platform 36',
    note: 'Plus current Build Tools.',
  },
  {
    label: 'Android NDK',
    value: '27.2.12479018',
    note: 'Set ANDROID_NDK_HOME when not under $ANDROID_HOME/ndk.',
  },
  {
    label: 'Rust',
    value: '3 Android targets',
    note: 'aarch64-linux-android, armv7-linux-androideabi, x86_64-linux-android.',
  },
] as const;

export const GRADLE_COMMANDS = [
  {
    command: '.\\gradlew.bat assembleDebug',
    note: 'Windows — debug APK with all three ABIs (~3.9 MB).',
  },
  {
    command: './gradlew assembleDebug testDebugUnitTest lintDebug',
    note: 'macOS / Linux — build, unit tests and lint.',
  },
  {
    command: 'cargo test --manifest-path app/src/main/rust/Cargo.toml',
    note: 'Native scorer tests on the host.',
  },
] as const;

export const ABI_TABLE = [
  { abi: 'arm64-v8a', target: 'aarch64-linux-android', pages: '16 KB aligned' },
  {
    abi: 'armeabi-v7a',
    target: 'armv7-linux-androideabi',
    pages: '4 KB layout',
  },
  { abi: 'x86_64', target: 'x86_64-linux-android', pages: '16 KB aligned' },
] as const;

export const ARCH_LAYERS = [
  {
    title: 'MainActivity',
    file: 'kotlin/.../MainActivity.kt',
    body: 'One immersive home activity: lifecycle, the short-lived package receiver, one worker executor, back handling and the diagnostics provider.',
  },
  {
    title: 'Data',
    file: 'data/*.kt',
    body: 'AppRepository (scoped launcher/Leanback queries), LauncherPreferences (favorites, order, recents, display), WallpaperStore (downsample and import).',
  },
  {
    title: 'UI',
    file: 'ui/*.kt',
    body: 'LauncherViews builds every screen from platform Views; TvUi and Palette supply focus drawables, lift animations and the three color roles.',
  },
  {
    title: 'Native',
    file: 'nativeengine/NativeEngine.kt + rust/src/lib.rs',
    body: 'JNI entry points for ranking and scoring. ProGuard rules keep the native bridges alive through R8.',
  },
] as const;

export const SCORE_TABLE = [
  { match: 'Exact label match', score: '1000' },
  { match: 'Label prefix', score: '800 − length penalty (max 100)' },
  { match: 'Word prefix', score: '650' },
  { match: 'Substring', score: '500' },
  { match: 'Ordered subsequence', score: '220 − gaps (max 100)' },
  { match: 'Pinned favorite bonus', score: '+100 − row position' },
  { match: 'Recent bonus', score: '+50 − row position' },
] as const;

export const DEPENDENCIES = [
  {
    title: 'One AndroidX artifact',
    body: 'androidx.activity only, for lifecycle-safe document-picker results and predictive-back handling. Dialogs use the platform AlertDialog.',
  },
  {
    title: 'One Rust crate',
    body: 'jni 0.21 with default features off, compiled with opt-level "s", LTO, one codegen unit and symbols stripped.',
  },
  {
    title: 'No UI framework',
    body: 'No Compose runtime, RecyclerView or Leanback. Every home view is constructed in Kotlin against the platform.',
  },
  {
    title: 'No network stack',
    body: 'No HTTP client, no analytics SDK, no crash reporter. The manifest declares no network permission at all.',
  },
] as const;
