import home from '@/assets/screenshots/home.png';
import search from '@/assets/screenshots/search.png';
import favorites from '@/assets/screenshots/favorites.png';
import settings from '@/assets/screenshots/settings.png';

export type Shot = {
  id: string;
  title: string;
  alt: string;
  caption: string;
  /** One extra factual sentence shown on the screenshots page only. */
  detail: string;
  image: ImageMetadata;
};

/**
 * Captured at 1920×1080 on a TV-sized display. Alt text describes what the
 * screen shows, not the decoration, so screen-reader users get the same gist.
 */
export const SHOTS: Shot[] = [
  {
    id: 'home',
    title: 'Home',
    alt: 'BlueLauncher home screen with a large hero card, category filter pills and a grid of poster-style app cards.',
    caption:
      'The hero card resumes the app you opened last. Category pills filter the grid below without leaving the home screen.',
    detail:
      'The grid renders from one scoped package query; hidden filter combinations are cleared rather than rebuilt, which is part of why the screen is ready in ~160 ms.',
    image: home,
  },
  {
    id: 'search',
    title: 'Search',
    alt: 'Search view listing ranked app matches for a typed query, ordered by the Rust scoring engine.',
    caption:
      'Exact matches rank first, then prefix, word-prefix, substring and fuzzy ordered matches. Favorites and recents get a boost.',
    detail:
      'Every candidate gets a text score — exact 1000, prefix 800, word-prefix 650, substring 500, subsequence 220 — plus a pin or recency bonus capped well below an exact match.',
    image: search,
  },
  {
    id: 'favorites',
    title: 'Favorites & recents',
    alt: 'Home screen showing a pinned favorites row and a recently opened row above the full app grid.',
    caption:
      'Hold OK on any app to pin it. Favorites keep a manual order, and recents stay bounded to twelve entries.',
    detail:
      'Pins and recents are plain preference entries, so the order survives reinstalls and the file stays a few kilobytes.',
    image: favorites,
  },
  {
    id: 'settings',
    title: 'Settings',
    alt: 'Settings dialog with sections for background theme, home screen sections, app card size and about information.',
    caption:
      'Everything is local: theme, wallpaper, clock format, card size and which sections appear on the home screen.',
    detail:
      'Each option maps to one preference key read at render time; there is no account and nothing to sync.',
    image: settings,
  },
];
