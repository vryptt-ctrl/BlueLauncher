import home from '@/assets/screenshots/home.png';
import search from '@/assets/screenshots/search.png';
import favorites from '@/assets/screenshots/favorites.png';
import settings from '@/assets/screenshots/settings.png';

export type Shot = {
  id: string;
  title: string;
  alt: string;
  caption: string;
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
    image: home,
  },
  {
    id: 'search',
    title: 'Search',
    alt: 'Search view listing ranked app matches for a typed query, ordered by the Rust scoring engine.',
    caption:
      'Exact matches rank first, then prefix, word-prefix, substring and fuzzy ordered matches. Favorites and recents get a boost.',
    image: search,
  },
  {
    id: 'favorites',
    title: 'Favorites & recents',
    alt: 'Home screen showing a pinned favorites row and a recently opened row above the full app grid.',
    caption:
      'Hold OK on any app to pin it. Favorites keep a manual order, and recents stay bounded to twelve entries.',
    image: favorites,
  },
  {
    id: 'settings',
    title: 'Settings',
    alt: 'Settings dialog with sections for background theme, home screen sections, app card size and about information.',
    caption:
      'Everything is local: theme, wallpaper, clock format, card size and which sections appear on the home screen.',
    image: settings,
  },
];
