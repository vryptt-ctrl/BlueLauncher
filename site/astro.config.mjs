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
  integrations: [
    sitemap({
      // Only real content pages; 404 stays out of the sitemap.
      filter: (page) => !page.includes('/404'),
      // Relative importance, home first then the install guide. No lastmod:
      // it would churn on every build without telling crawlers anything new.
      serialize(item) {
        const path = new URL(item.url).pathname;
        item.priority = path === '/' ? 1 : path === '/install/' ? 0.9 : 0.7;
        return item;
      },
    }),
  ],
  build: {
    format: 'directory',
    inlineStylesheets: 'auto',
  },
  devToolbar: { enabled: false },
});
