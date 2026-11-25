/** Feature copy, grouped the way the app itself groups them. */

export type Feature = {
  icon: string;
  title: string;
  body: string;
};

export type FeatureGroup = {
  id: string;
  kicker: string;
  title: string;
  lede: string;
  features: Feature[];
};

export const HIGHLIGHTS = [
  {
    icon: 'speed',
    stat: '~160 ms',
    label: 'internal UI ready',
    title: 'Fast startup',
    body: 'Programmatic platform Views only — no Compose, RecyclerView or Leanback runtime to warm up. The home screen is ready in about 160 ms.',
  },
  {
    icon: 'feather',
    stat: '< 1 MB',
    label: 'R8-shrunk release',
    title: 'Small APK',
    body: 'The debug APK is roughly 3.9 MB with three ABIs; release builds shrink below a megabyte.',
  },
  {
    icon: 'cpu',
    stat: 'Rust',
    label: 'search core via JNI',
    title: 'Rust-ranked search',
    body: 'Ranking and personalization scoring run natively, with a Kotlin fallback on unsupported ABIs.',
  },
  {
    icon: 'tv',
    stat: 'D-pad',
    label: 'first interaction',
    title: 'Built for the D-pad',
    body: 'Full directional navigation, focus lift with glow, spotlight pills and poster-style app cards.',
  },
] as const;

export const FEATURE_GROUPS: FeatureGroup[] = [
  {
    id: 'discover',
    kicker: 'Discover',
    title: 'Finding and launching apps',
    lede: 'Package discovery is scoped, installs are debounced, and search ranks results locally.',
    features: [
      {
        icon: 'apps',
        title: 'TV and launcher apps in one grid',
        body: 'Registers both LEANBACK_LAUNCHER and HOME intents, then discovers TV-launchable apps and regular launcher apps through scoped package visibility.',
      },
      {
        icon: 'search',
        title: 'Rust-ranked search',
        body: 'Exact, prefix, word-prefix, substring and ordered-subsequence matches each get their own score, with bonuses for favorites and recents.',
      },
      {
        icon: 'history',
        title: 'Bounded recents',
        body: 'The last twelve apps you opened stay one click away, most recent first, with no unbounded history log.',
      },
      {
        icon: 'offline',
        title: 'No network, no analytics',
        body: 'No network permission in the manifest, no ads, no database, no periodic services. Search and ranking never leave the device.',
      },
    ],
  },
  {
    id: 'organize',
    kicker: 'Organize',
    title: 'Organizing the grid',
    lede: 'Favorites keep a manual order and app categories filter the grid in place.',
    features: [
      {
        icon: 'pin',
        title: 'Ordered favorites',
        body: 'Hold OK to pin an app, then nudge it toward the start or end of the row. Pinned apps get a ranking boost during search too.',
      },
      {
        icon: 'sort',
        title: 'Manual app ordering',
        body: 'The saved order survives reinstalls: newly installed apps append, uninstalled ones drop out, your sequence stays intact.',
      },
      {
        icon: 'layers',
        title: 'Category filters',
        body: 'All apps, favorites, media and games are one pill row. Filtering reuses the loaded list instead of rebuilding the hidden rows.',
      },
      {
        icon: 'build',
        title: 'Debounced refresh',
        body: 'A lightweight receiver coalesces install and update broadcasts while the home screen is visible, plus one scan on resume as a fallback.',
      },
    ],
  },
  {
    id: 'personalize',
    kicker: 'Personalize',
    title: 'Themes and display options',
    lede: 'Every visual option is a local preference — no account, no sync, no cloud.',
    features: [
      {
        icon: 'palette',
        title: 'Three palettes',
        body: 'Blue nocturne, Midnight ink and Deep ocean each define background, surface, accent, glow and text roles for every painted view.',
      },
      {
        icon: 'image',
        title: 'Custom wallpaper',
        body: 'Import any image through the system document picker; it is downsampled to 1920×1080 in app-private storage with no storage permission needed.',
      },
      {
        icon: 'clock',
        title: 'Clock and date',
        body: 'Follows the system 12/24-hour format by default, with independent toggles for the clock, the date and the whole hero row.',
      },
      {
        icon: 'tune',
        title: 'Card size and labels',
        body: 'Pick compact, comfortable or large app cards, and hide labels entirely if you recognize the icons.',
      },
    ],
  },
];
