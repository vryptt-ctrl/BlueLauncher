# Changelog

All notable changes to BlueLauncher are documented in this file. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Redesigned home screen: layered gradient backdrop, taller hero card with accent glow, poster-style app cards with rounded icon tiles, spotlight-filled filter pills, and D-pad focus lift with glow shadow.
- Hero play button now resumes the most recently opened app (focusable, clickable, screen-reader label "Continue with {app}").
- Pinned-favorites row shows a discoverability hint ("Hold OK on an app to pin it here") when empty.
- Horizontal strips have fading edge indicators signaling more content.
- Search with no results now shows "No matches for {query}" instead of the generic empty-apps text.
- Performance: hidden favorites/recents rows are cleared instead of rebuilt during search rendering.
- Project documentation: CONTRIBUTING, changelog, issue/PR templates, CI workflow.
- `TvUi.blend` color-mixing helper; `Palette.glow`/`Palette.onAccent` roles and retuned palettes.

### Changed
- Focus animation adds translation-Z lift (6dp) and glow-tinted spot shadow (API 28+).
- Settings dialog sections use uppercase kickers and hairline dividers.

## [1.0.0] - 2026-11-25

### Added
- Initial release: native Android TV home launcher with Kotlin UI and Rust/JNI app ranking.
- HOME + LEANBACK_LAUNCHER intent integration, scoped package discovery with debounced refresh.
- Favorites (ordered), recently opened (bounded to 12), manual app ordering, category filters.
- Rust-powered ranked search with Kotlin fallback; three dark themes; custom image wallpaper import.
- 16 KB page alignment for arm64-v8a/x86_64; minSdk 23; R8-shrunk release builds.

[Unreleased]: https://github.com/vryptt-ctrl/BlueLauncher/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/vryptt-ctrl/BlueLauncher/releases/tag/v1.0.0
