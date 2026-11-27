/**
 * Single source of truth for links, version numbers and navigation.
 * Every fact here is copied from README.md, CHANGELOG.md, app/build.gradle.kts
 * or app/src/main/res/values/strings.xml — update those first, then this file.
 */

export const SITE = {
  name: 'BlueLauncher',
  tagline: 'A lightweight, offline home screen for Android TV.',
  description:
    'A lightweight, open-source home launcher for Android TV. Fast D-pad navigation, favorites, search and themes — fully offline, with no ads and no analytics.',
  version: '1.1.2',
  versionDate: '2026-11-26',
  releaseDate: '2026-11-26',
  /** APK attached to the latest GitHub release, and its measured size. */
  releaseApk: 'BlueLauncher-v1.1.2-release.apk',
  releaseApkSize: 'about 1.2 MB',
  /** Debug build: all three ABIs, unshrunk. */
  debugApkSize: 'about 3.9 MB',
  packageId: 'com.bluelabs.bluelauncher',
  minSdk: 23,
  /** Human-readable release name for minSdk, for copy that reads to users. */
  minSdkName: '6.0 Marshmallow',
  /** Just the number, for tight UI chips. */
  minSdkShort: '6.0',
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
    'open source launcher',
    'lightweight launcher',
    'no ads launcher',
    'sideload APK',
  ],
} as const;

export type NavItem = { label: string; href: string };

export const NAV: NavItem[] = [
  { label: 'Features', href: '/#features' },
  { label: 'Install', href: '/install/' },
  { label: 'Developers', href: '/developers/' },
];

export const FOOTER_LINKS: { title: string; links: NavItem[] }[] = [
  {
    title: 'Project',
    links: [
      { label: 'Install', href: '/install/' },
      { label: 'Developers', href: '/developers/' },
      { label: 'Changelog', href: SITE.changelog },
      { label: 'Releases', href: SITE.allReleases },
      { label: 'GitHub', href: SITE.repo },
    ],
  },
  {
    title: 'Support',
    links: [
      { label: 'Report a bug', href: SITE.issueTemplates },
      { label: 'Security policy', href: SITE.security },
      { label: 'License: GPL-3.0', href: SITE.licenseCode },
    ],
  },
];
