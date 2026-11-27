/**
 * Base-path aware link builder.
 *
 * The site is published at the root of the custom domain
 * https://bluelauncher.bond (GitHub Pages + CNAME), so `BASE_URL` is `/` and
 * links come out root-relative. The prefix still flows through `url()` so a
 * future `base` in astro.config.mjs would apply everywhere at once.
 */
const RAW_BASE = import.meta.env.BASE_URL || '/';
const BASE = RAW_BASE.endsWith('/') ? RAW_BASE : `${RAW_BASE}/`;

/** Paths that resolve to a real file (or carry a fragment) are passed through. */
function isAssetPath(path: string): boolean {
  if (path.includes('#')) return true;
  if (path.startsWith('mailto:') || path.startsWith('http')) return true;
  return /\.[a-z0-9]+$/i.test(path);
}

/**
 * `url('install/#adb')` → `/install/#adb`
 * `url('/')` → `/`
 * `url('/favicon.svg')` → `/favicon.svg`
 */
export function url(path = '/'): string {
  if (/^(https?:|mailto:|tel:)/.test(path)) return path;
  const clean = path.replace(/^\/+/, '');
  if (clean === '') return BASE;
  if (isAssetPath(clean)) return `${BASE}${clean}`;
  return `${BASE}${clean.replace(/\/+$/, '')}/`;
}

/** Absolute URL for canonical tags and social metadata. */
export function absoluteUrl(path = '/'): string {
  const site = (import.meta.env.SITE || 'https://bluelauncher.bond').replace(/\/+$/, '');
  return `${site}${url(path)}`;
}

/** Marks the active navigation item, tolerating the trailing slash. */
export function isActive(current: string, href: string): boolean {
  const norm = (value: string) => value.replace(/\/+$/, '') || '/';
  return norm(current) === norm(href);
}
