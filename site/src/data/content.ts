/** Install, build and architecture copy. Commands are copied from README.md. */
import { SITE } from '@/lib/site';

export type Step = {
  title: string;
  body: string;
  code?: string;
  lang?: string;
  note?: string;
};

export const INSTALL_STEPS: Step[] = [
  {
    title: 'Download the APK',
    body: `Get the latest release from GitHub (${SITE.releaseApkSize}). Releases are signed by the maintainer. You can download it on the TV itself, or on a phone or PC.`,
    note: 'Requires Android 6.0 (API 23) or newer.',
  },
  {
    title: 'Install it on the TV',
    body: 'Copy the APK to the TV with a USB drive or a file-transfer app, open it in a file manager and install. If the TV blocks the install, allow installs from unknown sources for that file manager when asked.',
  },
  {
    title: 'Make it the home screen',
    body: 'Press the Home button and choose BlueLauncher in the Home-app chooser. Pick “Always” if your TV offers it, so it stays the default.',
  },
];

export const ADB_STEPS: Step[] = [
  {
    title: 'Install over adb',
    body: 'Enable ADB debugging in the TV\u2019s developer options, connect, then install the downloaded APK.',
    code: `adb connect <tv-ip>:5555\nadb install -r ${SITE.releaseApk}`,
    lang: 'shell',
  },
  {
    title: 'Set it as the default home app',
    body: 'Use this if your TV does not show the Home-app chooser.',
    code: 'adb shell cmd package set-home-activity com.bluelabs.bluelauncher/.MainActivity',
    lang: 'shell',
  },
];

export const INSTALL_TROUBLESHOOTING = [
  {
    title: 'The TV blocks the install',
    body: 'Allow installs from unknown sources for the file manager you are using. The setting is usually under Settings > Device Preferences > Security & restrictions, though the path varies by device.',
  },
  {
    title: 'BlueLauncher is not offered as a home app',
    body: 'Some vendor launchers cache the home chooser. Clear that launcher\u2019s data, or use the adb command above.',
  },
  {
    title: 'Some apps are missing from the grid',
    body: 'Only apps that declare a TV or launcher icon are listed. An app with neither will not appear.',
  },
] as const;

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
