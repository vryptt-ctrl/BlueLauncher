/**
 * Changelog mirror of CHANGELOG.md (Keep a Changelog + Semantic Versioning).
 * Update here after editing the root file.
 */

export type ChangeGroup = {
  kind: 'Added' | 'Changed' | 'Fixed' | 'Removed';
  items: string[];
};
export type Release = {
  version: string;
  date: string;
  summary: string;
  groups: ChangeGroup[];
  latest?: boolean;
};

export const RELEASES: Release[] = [
  {
    version: '1.1.1',
    date: '2026-11-25',
    latest: true,
    summary:
      'Focus and layout fixes: rows stay inside their viewport, the focused card keeps its full accent stroke at both ends, and D-pad focus no longer jumps between rows.',
    groups: [
      {
        kind: 'Added',
        items: [
          'Marketing and documentation site under site/, published to GitHub Pages.',
          'About panel now shows the version from the build instead of a hard-coded string.',
        ],
      },
      {
        kind: 'Fixed',
        items: [
          'App cards no longer paint across the screen gutter while a row is scrolled. Each horizontal strip sits in its own clipping container, so scrolled-out cards are cut at the row edge while the hero, search pill and category chips still draw their focus-scale overflow.',
          'The focused card keeps its full accent stroke at both ends of every strip: strips have 10 dp of horizontal headroom, the scroll views are pulled outwards by the same amount, and the fading-edge length was shortened to match.',
          'The “Large app cards” setting no longer clips the bottom stroke of the focused card; row heights are derived from the card size.',
          'Focus no longer jumps between rows when a strip is rebuilt (favorite toggled, order changed).',
          'Left/right presses at either end of a strip stay inside that row, and the first Down press lands on the main apps strip.',
          'A focused card is kept fully on screen vertically, including its focus-scale overflow.',
        ],
      },
    ],
  },
  {
    version: '1.1.0',
    date: '2026-11-25',
    summary:
      'Official Focus Grid brand identity: adaptive icon with themed support, PNG mipmaps for older devices, a dedicated TV banner and committed brand sources.',
    groups: [
      {
        kind: 'Added',
        items: [
          'Official “Focus Grid” brand identity: adaptive launcher icon with Android 13+ themed (monochrome) support, PNG mipmaps for Android 6.x–7.x devices, and a dedicated 320×180 TV banner.',
          'Round launcher icon variant for masked launchers.',
          'Brand sources (SVG, logos, Play Store icon, social preview) committed under brand/ for future edits and store listings.',
          'Brand assets licensed separately under CC BY 4.0; the code remains GPL-3.0.',
          'README now shows the logo, a four-shot screenshot gallery and a branding section.',
        ],
      },
      {
        kind: 'Changed',
        items: [
          'Manifest points at @mipmap/ic_launcher with roundIcon; the temporary vector placeholder icon and vector TV banner were removed.',
          'Screenshots retaken against the final branded build.',
        ],
      },
    ],
  },
  {
    version: '1.0.0',
    date: '2026-11-25',
    summary:
      'First public release: a native Android TV home launcher with Kotlin UI and a Rust/JNI ranking core.',
    groups: [
      {
        kind: 'Added',
        items: [
          'Redesigned home screen: layered gradient backdrop, taller hero card with accent glow, poster-style app cards with rounded icon tiles, spotlight-filled filter pills, and D-pad focus lift with glow shadow.',
          'Hero play button resumes the most recently opened app, focusable and labelled “Continue with {app}” for screen readers.',
          'Pinned-favorites row shows a discoverability hint (“Hold OK on an app to pin it here”) when empty.',
          'Horizontal strips have fading edge indicators signaling more content.',
          'Search with no results shows “No matches for {query}” instead of generic empty-apps text.',
          'HOME + LEANBACK_LAUNCHER intent integration, scoped package discovery with debounced refresh.',
          'Favorites (ordered), recently opened (bounded to 12), manual app ordering, category filters.',
          'Rust-powered ranked search with Kotlin fallback; three dark themes; custom image wallpaper import.',
          '16 KB page alignment for arm64-v8a and x86_64; minSdk 23; R8-shrunk release builds.',
          'Project documentation: CONTRIBUTING, changelog, issue and PR templates, CI workflow.',
        ],
      },
      {
        kind: 'Changed',
        items: [
          'Focus animation adds translation-Z lift (6dp) and a glow-tinted spot shadow on API 28+.',
          'Settings dialog sections use uppercase kickers and hairline dividers.',
          'Performance: hidden favorites and recents rows are cleared instead of rebuilt during search rendering.',
          'TvUi.blend color-mixing helper; Palette.glow and Palette.onAccent roles, retuned palettes.',
        ],
      },
    ],
  },
];
