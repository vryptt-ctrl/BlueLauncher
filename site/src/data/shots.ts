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

/** Captured at 1920×1080. Alt text describes what the screen shows. */
export const SHOTS: Shot[] = [
  {
    id: 'home',
    title: 'Home',
    alt: 'BlueLauncher home screen with a large hero card, category filter pills and a grid of poster-style app cards.',
    caption: 'The hero card resumes your last app. Category pills filter the grid in place.',
    image: home,
  },
  {
    id: 'search',
    title: 'Search',
    alt: 'Search view listing ranked app matches for a typed query.',
    caption: 'Best matches first, with favorites and recent apps ranked higher.',
    image: search,
  },
  {
    id: 'favorites',
    title: 'Favorites',
    alt: 'Home screen showing a pinned favorites row and a recently opened row above the full app grid.',
    caption: 'Hold OK on any app to pin it. Recents keep the last twelve apps.',
    image: favorites,
  },
  {
    id: 'settings',
    title: 'Settings',
    alt: 'Settings dialog with sections for background theme, home screen sections, app card size and about information.',
    caption: 'Theme, wallpaper, clock, card size and which sections appear on the home screen.',
    image: settings,
  },
];
