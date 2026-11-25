// @ts-check
import { defineConfig } from 'astro/config';
import sitemap from '@astrojs/sitemap';

/**
 * Published to GitHub Pages with the custom domain https://bluelauncher.bond/
 * (a CNAME file in public/ keeps the domain binding across deploys). Internal
 * links still go through `url()` in src/lib/url.ts so a future `base` change
 * never requires touching templates.
 */
export default defineConfig({
  site: 'https://bluelauncher.bond',
  trailingSlash: 'always',
  output: 'static',
  integrations: [sitemap()],
  build: {
    format: 'directory',
    inlineStylesheets: 'auto',
  },
  devToolbar: { enabled: false },
});
