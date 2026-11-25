# BlueLauncher website

Static marketing and documentation site for BlueLauncher, built with [Astro](https://astro.build).
No UI framework, no client-side routing, no third-party requests at runtime.

Published as a GitHub Pages **project** page:

- <https://vryptt-ctrl.github.io/BlueLauncher/>

## Commands

```sh
npm ci          # install exact dependencies from package-lock.json
npm run dev     # dev server on http://localhost:4321/BlueLauncher/
npm run build   # static output in dist/
npm run preview # serve dist/ on http://localhost:4321/BlueLauncher/
npm run check   # astro check (types + templates)
```

Node 22.23.4 or newer is required by Astro 7; CI uses Node 24.

## Deployment

`.github/workflows/pages.yml` builds the site on every push to `main` that touches `site/`, then
uploads `site/dist` as a Pages artifact with `actions/upload-pages-artifact` and deploys it with
`actions/deploy-pages`. The repository must have **Settings → Pages → Source: GitHub Actions** set,
and the deploy job needs the `github-pages` environment that the workflow declares.

## Structure

```
site/
├─ astro.config.mjs       # site, base, trailingSlash, sitemap integration
├─ public/                # copied verbatim: brand files, favicon, robots.txt, webmanifest
└─ src/
   ├─ assets/             # screenshots and self-hosted Inter (processed by astro:assets)
   ├─ components/         # Nav, Footer, Button, Gallery, CodeBlock, TVFrame, …
   ├─ data/               # copy: features, install/build steps, changelog, palette, icons
   ├─ layouts/Base.astro  # head metadata, skip link, nav, main, footer
   ├─ lib/                # site.ts (facts/links), url.ts (base-aware hrefs)
   ├─ pages/              # one file per route; 404.astro for the error page
   └─ styles/             # tokens.css (design tokens), base.css (reset + primitives)
```

## Rules worth keeping

- **Every internal link goes through `url()`** from `src/lib/url.ts`. The site is served from
  `/BlueLauncher/`, so hardcoding `/install/` breaks the deploy. The same applies to `public/`
  assets: use `url('/brand/icon.svg')`, never a bare path.
- **Facts live in `src/data/` and `src/lib/site.ts`.** The version, SDK levels, NDK version, ABI
  table and changelog entries are mirrored from `app/build.gradle.kts`, `CHANGELOG.md` and
  `README.md`. Change those files first, then this copy.
- **One theme.** Colors derive from `ui/Palette.kt` (Blue nocturne) and `brand/README.md`. There is
  deliberately no light mode or theme switcher.
- **Self-hosted fonts.** Inter 300/400/700 are copied from `app/src/main/assets/fonts` so the site
  makes zero third-party requests, matching the launcher's offline claim.
- **Images go through `astro:assets`.** `Picture` with `formats={['avif', 'webp']}` and
  `fallbackFormat="jpg"` produces the responsive set; plain `Image` does not accept `formats`.
- **No invented numbers.** Timings, sizes and scores are copied from the README or the source. If a
  claim cannot be sourced, it does not belong on the site.

## Adding a page

1. Create `src/pages/name.astro` and wrap the content in `<Base title="…" description="…">`.
2. Add the route to `NAV` or `FOOTER_LINKS` in `src/lib/site.ts` if it should be reachable.
3. Use `SectionHead` for section headings and `url('/name/')` for links to it.
4. Run `npm run check && npm run build` before committing.
