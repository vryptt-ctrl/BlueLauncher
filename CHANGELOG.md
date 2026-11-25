# Changelog

All notable changes to BlueLauncher are documented in this file. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.1] - 2026-11-25

### Added
- Marketing and documentation site under `site/` (static Astro build, self-hosted fonts, no third-party requests), published to GitHub Pages by `.github/workflows/pages.yml`.
- About panel now shows the version from the build instead of a hard-coded string.

### Fixed
- App cards no longer paint across the screen gutter while a row is scrolled. Each horizontal strip now sits in its own clipping container, so scrolled-out cards are cut at the row edge while the hero, search pill and category chips still draw their focus-scale overflow.
- The focused card keeps its full accent stroke at both ends of every strip: strips have 10 dp of horizontal headroom, the scroll views are pulled outwards by the same amount so cards still align with the content margin, and the fading-edge length was shortened to match.
- The "Large app cards" setting no longer clips the bottom stroke of the focused card; row heights are derived from the card size and re-applied when the setting changes.
- Focus no longer jumps between rows when a strip is rebuilt (favorite toggled, order changed): the rebuild restores the cursor to the same app, or the nearest position if that app left the list.
- Left/right presses at either end of a strip stay inside that row instead of leaving for an unrelated row.
- The first Down press from the root view now lands on the main apps strip instead of the bottom row.
- A focused card is kept fully on screen vertically, including its focus-scale overflow.

## [1.1.0] - 2026-11-25

### Added
- Official "Focus Grid" brand identity: adaptive launcher icon with Android 13+ themed (monochrome) support, PNG mipmaps for Android 6.x–7.x devices, and a dedicated 320×180 TV banner.
- Round launcher icon variant for masked launchers.
- Brand sources (SVG, logos, Play Store icon, social preview) committed under `brand/` for future edits and store listings.
- Brand assets licensed separately under CC BY 4.0 (`brand/LICENSE-ASSETS.md`); the code remains GPL-3.0.
- README now shows the logo, a four-shot screenshot gallery and a branding section.

### Changed
- Manifest points at `@mipmap/ic_launcher` with `roundIcon`; the temporary vector placeholder icon and vector TV banner were removed.
- Screenshots retaken against the final branded build.

## [1.0.0] - 2026-11-25

### Added
- Initial release: native Android TV home launcher with Kotlin UI and Rust/JNI app ranking.
- Redesigned home screen: layered gradient backdrop, taller hero card with accent glow, poster-style app cards with rounded icon tiles, spotlight-filled filter pills, and D-pad focus lift with glow shadow.
- Hero play button now resumes the most recently opened app (focusable, clickable, screen-reader label "Continue with {app}").
- Pinned-favorites row shows a discoverability hint ("Hold OK on an app to pin it here") when empty.
- Horizontal strips have fading edge indicators signaling more content.
- Search with no results now shows "No matches for {query}" instead of the generic empty-apps text.
- Performance: hidden favorites/recents rows are cleared instead of rebuilt during search rendering.
- Project documentation: CONTRIBUTING, changelog, issue/PR templates, CI workflow.
- `TvUi.blend` color-mixing helper; `Palette.glow`/`Palette.onAccent` roles and retuned palettes.
- HOME + LEANBACK_LAUNCHER intent integration, scoped package discovery with debounced refresh.
- Favorites (ordered), recently opened (bounded to 12), manual app ordering, category filters.
- Rust-powered ranked search with Kotlin fallback; three dark themes; custom image wallpaper import.
- 16 KB page alignment for arm64-v8a/x86_64; minSdk 23; R8-shrunk release builds.

### Changed
- Focus animation adds translation-Z lift (6dp) and glow-tinted spot shadow (API 28+).
- Settings dialog sections use uppercase kickers and hairline dividers.

[Unreleased]: https://github.com/vryptt-ctrl/BlueLauncher/compare/v1.1.1...HEAD
[1.1.1]: https://github.com/vryptt-ctrl/BlueLauncher/compare/v1.1.0...v1.1.1
[1.1.0]: https://github.com/vryptt-ctrl/BlueLauncher/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/vryptt-ctrl/BlueLauncher/releases/tag/v1.0.0
