/**
 * Single source of truth for links, version numbers and navigation.
 * Every fact here is copied from README.md, CHANGELOG.md, app/build.gradle.kts
 * or app/src/main/res/values/strings.xml — update those first, then this file.
 */

export const SITE = {
  name: 'BlueLauncher',
  tagline: 'A small, offline home screen for Android TV.',
  description:
    'Free, open-source Android TV home launcher. Kotlin UI, Rust-ranked app search, fully offline — no ads, no analytics, no network permission.',
  version: '1.1.1',
  versionDate: '2026-11-25',
  releaseDate: '2026-11-25',
  /** APK attached to the latest GitHub release, and its measured size. */
  releaseApk: 'BlueLauncher-v1.1.1-release.apk',
  releaseApkSize: 'about 1.2 MB',
  /** Debug build: all three ABIs, unshrunk. */
  debugApkSize: 'about 3.9 MB',
  packageId: 'com.bluelabs.bluelauncher',
  minSdk: 23,
  compileSdk: 36,
  ndkVersion: '27.2.12479018',
  jdk: 17,
  repo: 'https://github.com/vryptt-ctrl/BlueLauncher',
  releases: 'https://github.com/vryptt-ctrl/BlueLauncher/releases/latest',
  allReleases: 'https://github.com/vryptt-ctrl/BlueLauncher/releases',
  issues: 'https://github.com/vryptt-ctrl/BlueLauncher/issues',
  issueTemplates: 'https://github.com/vryptt-ctrl/BlueLauncher/issues/new/choose',
  security: 'https://github.com/vryptt-ctrl/BlueLauncher/blob/main/SECURITY.md',
  contributing: 'https://github.com/vryptt-ctrl/BlueLauncher/blob/main/CONTRIBUTING.md',
  licenseCode: 'https://github.com/vryptt-ctrl/BlueLauncher/blob/main/LICENSE',
  licenseAssets: 'https://github.com/vryptt-ctrl/BlueLauncher/blob/main/brand/LICENSE-ASSETS.md',
  changelog: 'https://github.com/vryptt-ctrl/BlueLauncher/blob/main/CHANGELOG.md',
  ogImage: '/brand/social-preview-1280x640.png',
  /**
   * Search-facing topic list. Feeds schema.org `keywords` and the default
   * `<meta name="keywords">`, so keep the phrases people actually type.
   */
  keywords: [
    'Android TV launcher',
    'Android TV home screen',
    'Android TV box launcher',
    'open source Android launcher',
    'lightweight launcher',
    'offline launcher',
    'no ads launcher',
    'Kotlin launcher',
    'Rust JNI',
    'Leanback launcher',
    'launcher APK',
    'sideload APK',
  ],
} as const;

export type NavItem = { label: string; href: string };

export const NAV: NavItem[] = [
  { label: 'Features', href: '/features/' },
  { label: 'Screenshots', href: '/screenshots/' },
  { label: 'Install', href: '/install/' },
  { label: 'Build', href: '/build/' },
  { label: 'Changelog', href: '/changelog/' },
];

export const FOOTER_LINKS: { title: string; links: NavItem[] }[] = [
  {
    title: 'Product',
    links: [
      { label: 'Features', href: '/features/' },
      { label: 'Screenshots', href: '/screenshots/' },
      { label: 'Install on a TV', href: '/install/' },
      { label: 'Changelog', href: '/changelog/' },
    ],
  },
  {
    title: 'Developers',
    links: [
      { label: 'Build from source', href: '/build/' },
      { label: 'Architecture', href: '/architecture/' },
      { label: 'Contributing', href: SITE.contributing },
      { label: 'Report a bug', href: SITE.issueTemplates },
    ],
  },
  {
    title: 'Project',
    links: [
      { label: 'GitHub repository', href: SITE.repo },
      { label: 'Releases', href: SITE.allReleases },
      { label: 'Security policy', href: SITE.security },
    ],
  },
  {
    title: 'Legal',
    links: [
      { label: 'Licenses', href: '/license/' },
      { label: 'Code: GPL-3.0', href: SITE.licenseCode },
    ],
  },
];
