/**
 * Inline SVG icons on a 24×24 grid, drawn with a 1.75px stroke so they match
 * the hairline borders used across the site. Keeping them inline avoids an
 * extra font request and lets each icon inherit `currentColor`.
 */
export type IconName =
  | 'search'
  | 'settings'
  | 'apps'
  | 'star'
  | 'history'
  | 'bolt'
  | 'feather'
  | 'cpu'
  | 'tv'
  | 'palette'
  | 'tune'
  | 'offline'
  | 'download'
  | 'build'
  | 'terminal'
  | 'check'
  | 'copy'
  | 'arrow'
  | 'external'
  | 'shield'
  | 'code'
  | 'brush'
  | 'pin'
  | 'sort'
  | 'image'
  | 'clock'
  | 'speed'
  | 'layers'
  | 'github'
  | 'menu'
  | 'close'
  | 'chevron';

export const ICON_PATHS: Record<IconName, string> = {
  search: '<circle cx="11" cy="11" r="6.5"/><path d="m16 16 4.5 4.5"/>',
  settings:
    '<circle cx="12" cy="12" r="2.75"/><path d="M12 3.5v2M12 18.5v2M3.5 12h2M18.5 12h2M6 6l1.4 1.4M16.6 16.6 18 18M18 6l-1.4 1.4M7.4 16.6 6 18"/>',
  apps: '<rect x="4" y="4" width="7" height="7" rx="2"/><rect x="13" y="4" width="7" height="7" rx="2"/><rect x="4" y="13" width="7" height="7" rx="2"/><rect x="13" y="13" width="7" height="7" rx="2"/>',
  star: '<path d="m12 4 2.5 5.2 5.5.8-4 3.9 1 5.6-5-2.7-5 2.7 1-5.6-4-3.9 5.5-.8Z"/>',
  history:
    '<path d="M4 12a8 8 0 1 0 2.5-5.8"/><path d="M4 4.5V9h4.5"/><path d="M12 8.5V12l2.75 2"/>',
  bolt: '<path d="M13.5 3 6 13.5h4.5L10 21l7.5-10.5H13Z"/>',
  feather: '<path d="M20 4c0 6-4 9-8 11l-5 5"/><path d="M20 4C13 4 7 8 5 15"/><path d="M9 15h5"/>',
  cpu: '<rect x="7" y="7" width="10" height="10" rx="2.5"/><path d="M10 3.5v2M14 3.5v2M10 18.5v2M14 18.5v2M3.5 10h2M3.5 14h2M18.5 10h2M18.5 14h2"/>',
  tv: '<rect x="3" y="5" width="18" height="12" rx="2.5"/><path d="M9 21h6M12 17v4"/>',
  palette:
    '<path d="M12 3.5a8.5 8.5 0 1 0 0 17c1.4 0 2-.9 2-1.9 0-1.4-1.2-1.9-1.2-3 0-1 .8-1.6 1.9-1.6h1.6A4.2 4.2 0 0 0 20.5 10c0-3.8-3.8-6.5-8.5-6.5Z"/><circle cx="8" cy="10" r="1"/><circle cx="12" cy="7.75" r="1"/><circle cx="16" cy="10" r="1"/>',
  tune: '<path d="M5 8h9M18 8h1M5 16h4M13 16h6"/><circle cx="15.5" cy="8" r="2"/><circle cx="10.5" cy="16" r="2"/>',
  offline:
    '<path d="M5 15a4 4 0 0 1 .6-8 6 6 0 0 1 11.2 1.3A3.5 3.5 0 0 1 18 15"/><path d="m4 4 16 16"/>',
  download: '<path d="M12 4v10"/><path d="m8 11 4 4 4-4"/><path d="M5 19h14"/>',
  build: '<path d="m14 6 4 4-8.5 8.5-4-4Z"/><path d="m13 4 7 7"/>',
  terminal:
    '<rect x="3.5" y="4.5" width="17" height="15" rx="2.5"/><path d="m7.5 10 2.5 2-2.5 2M12.5 14h4"/>',
  check: '<circle cx="12" cy="12" r="8.5"/><path d="m8.5 12 2.5 2.5 4.5-5"/>',
  copy: '<rect x="9" y="9" width="11" height="11" rx="2"/><path d="M15 6.5A2.5 2.5 0 0 0 12.5 4h-6A2.5 2.5 0 0 0 4 6.5v6A2.5 2.5 0 0 0 6.5 15"/>',
  arrow: '<path d="M5 12h13"/><path d="m13 6 6 6-6 6"/>',
  external:
    '<path d="M14 4h6v6"/><path d="m20 4-8 8"/><path d="M18 14v4a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4"/>',
  shield:
    '<path d="M12 3.5 19 6v5.5c0 4.4-3 7.3-7 9-4-1.7-7-4.6-7-9V6Z"/><path d="m9 12 2 2 4-4"/>',
  code: '<path d="m8.5 8-4.5 4 4.5 4"/><path d="m15.5 8 4.5 4-4.5 4"/>',
  brush:
    '<path d="M14.5 4.5 19.5 9.5 18 11l-5-5Z"/><path d="m13.5 6.5-7 7V18h4.5l7-7"/><path d="M4 20.5c1.6.4 3-.4 3-1.8 0-1-.7-1.7-1.7-1.7-1 0-1.8.8-1.8 1.9 0 .7.2 1.2.5 1.6Z"/>',
  pin: '<path d="M9 3.5h6l-1 5 3.5 3.5H6.5L10 8.5Z"/><path d="M12 12v8.5"/>',
  sort: '<path d="M7 4v16"/><path d="m4 17 3 3 3-3"/><path d="M17 20V4"/><path d="m14 7 3-3 3 3"/>',
  image:
    '<rect x="3.5" y="5" width="17" height="14" rx="2.5"/><circle cx="9" cy="10.5" r="1.6"/><path d="m4.5 17 4.5-4 3 2.5 3-3 4.5 4.5"/>',
  clock: '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/>',
  speed:
    '<path d="M4.5 17a8.5 8.5 0 1 1 15 0"/><path d="m12 13 4-4"/><circle cx="12" cy="14" r="1.5"/>',
  layers: '<path d="m12 3.5 8.5 4.5L12 12.5 3.5 8Z"/><path d="m3.5 12.5 8.5 4.5 8.5-4.5"/>',
  github:
    '<path d="M9 20.5c-3.5 1-4-1.5-5.5-2M15 21v-3.4a2.6 2.6 0 0 0-.7-2c2.4-.3 4.7-1.2 4.7-5.3a4 4 0 0 0-1.1-2.8 3.7 3.7 0 0 0-.1-2.8s-1-.3-3.3 1.2a8.4 8.4 0 0 0-4.6 0C6.6 3.4 5.6 3.7 5.6 3.7a3.7 3.7 0 0 0-.1 2.8A4 4 0 0 0 4.4 9.3c0 4 2.3 5 4.7 5.3a2.6 2.6 0 0 0-.7 2V21"/>',
  menu: '<path d="M4 7h16M4 12h16M4 17h16"/>',
  close: '<path d="m6 6 12 12M18 6 6 18"/>',
  chevron: '<path d="m9 6 6 6-6 6"/>',
};

export const GLYPH_FILLED: IconName[] = ['star', 'bolt', 'pin'];
