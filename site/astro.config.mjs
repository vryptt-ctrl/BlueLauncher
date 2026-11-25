// @ts-check
import { defineConfig } from 'astro/config';
import sitemap from '@astrojs/sitemap';

/**
 * GitHub Pages project site: https://vryptt-ctrl.github.io/BlueLauncher/
 * `base` must stay in sync with the repository name; every internal link is
 * built through `url()` in src/lib/url.ts so the prefix is never hardcoded.
 */
export default defineConfig({
  site: 'https://vryptt-ctrl.github.io',
  base: '/BlueLauncher',
  trailingSlash: 'always',
  output: 'static',
  integrations: [sitemap()],
  build: {
    format: 'directory',
    inlineStylesheets: 'auto',
  },
  devToolbar: { enabled: false },
});
