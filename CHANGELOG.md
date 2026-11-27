# Changelog

All notable changes to BlueLauncher are documented in this file. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.3.0] - 2026-11-27

### Added
- Redesigned Settings dialog with interactive accent color swatches, modern TV switch toggles, and segmented size picker for grid card sizing.
- Architecture and build documentation in `AGENTS.md`.

### Fixed
- Eliminated D-pad focus frame drops and scroll jank on app strips.

### Changed
- Polished TV launcher UI styling: refined hero resume card, search bar focus indicators, and dialog animations.

### Added
- Modular UI architecture: extracted components (`AppStripView`, `HeroView`, `SearchBarView`) and dialogs (`SettingsDialog`, `AboutDialog`, `PanelDialog`) out of `LauncherViews` for better maintainability and extensibility.
- `LauncherViewModel` and `LauncherFilterEngine` to separate presentation, state management, and filtering/ranking logic from `MainActivity`.
- Unit test suite for `LauncherFilterEngine`.

### Changed
- The website is now four pages (home, install, developers, 404) instead of ten. Feature groups and
  score tables moved into home-page sections, the build and architecture pages merged into
  `/developers/`, and the changelog and licence pages now link to the repository files.
- `/features/`, `/screenshots/`, `/build/`, `/architecture/`, `/changelog/` and `/license/` redirect
  to their new homes, so indexed URLs keep resolving.
- The footer credits both licences again: code under GPL-3.0 and brand assets under CC BY 4.0.

### Removed
- `public/brand/` files the site never referenced (SVG marks, wordmarks, the Play Store icon); the
  favicon, `icon-512.png` and the social preview remain.
- Home-page copy that repeated README numbers in three places (chips, highlight cards, install
  panel). The facts now appear once, in the stats strip and the install steps.

## [1.1.2] - 2026-11-26

### Fixed
- A settings or long-press panel now always dismisses; a panel whose caller passed no dismiss action used to fade out but leave its invisible scrim attached, swallowing every key press.
- Settings rows repaint immediately after a toggle: the check mark and accent colour are read back from the live preference instead of being painted once when the panel opened.
- Icon and text share a label again. Only the private-use glyph codepoints are rendered with the bundled symbol font and the words stay in Inter, so headings and chips no longer show tofu boxes or Roboto fallback.
- Typing in search no longer replays the card entrance animation on every keystroke, which looked like flicker on low-end boxes.
- Imported wallpapers are center-cropped to the screen aspect when drawn, and each import deletes the previous JPEG. Switching back to a built-in wallpaper removes the imported file instead of leaving it behind.
- The resume badge no longer points at whatever survived the current search or category filter; the target is resolved against every installed app.
- Reordering a pinned favourite no longer drops pins whose app is temporarily uninstalled, and it swaps the two slots so a hidden pin keeps its position.
- The Media category no longer swallows the system TV settings and the remote; hints match the package name only and generic words like "tv" and "player" are gone.
- The resume badge pulse runs a fixed number of breaths and stops on pause, instead of invalidating the view forever.
- Hero decoration repaints after a theme change instead of keeping the previous palette's tint.
- The category chip keeps its selected fill when the cursor moves to a sibling chip.
- A package install or update no longer steals focus out of the search box, and pressing Home again only rescans when something actually changed.
- The clock keeps the system's own 24-hour pattern, so locales that use a separator such as "HH.mm" still look native.
- The native search library degrades to the Kotlin fallback on any load failure, and labels containing tabs or newlines no longer corrupt the JNI payload.
- Release builds use the real bundled Inter 700 and 300 weights instead of synthesised bold and a 400 substitute.

### Changed
- Renders triggered from the settings panel, from launching an app, or from a palette change skip the grid entrance animation.
- Dead code and one unused string were removed (`onboardingComplete`, `saveAppOrder`, `roundIcon`, `TextView.colors`, `wallpaperEnd`, `GLYPH_SECTION_*`, `favorites_hint_title`).
- The Rust build task now tracks `Cargo.lock` and appends to `RUSTFLAGS` instead of overwriting a caller's value.
- README and site copy describe the custom `Dialog` panels rather than the platform `AlertDialog`.

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

[Unreleased]: https://github.com/vryptt-ctrl/BlueLauncher/compare/v1.3.0...HEAD
[1.3.0]: https://github.com/vryptt-ctrl/BlueLauncher/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/vryptt-ctrl/BlueLauncher/compare/v1.1.2...v1.2.0
[1.1.2]: https://github.com/vryptt-ctrl/BlueLauncher/compare/v1.1.1...v1.1.2
[1.1.1]: https://github.com/vryptt-ctrl/BlueLauncher/compare/v1.1.0...v1.1.1
[1.1.0]: https://github.com/vryptt-ctrl/BlueLauncher/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/vryptt-ctrl/BlueLauncher/releases/tag/v1.0.0
