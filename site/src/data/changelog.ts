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
    version: '1.1.2',
    date: '2026-11-26',
    latest: true,
    summary:
      'Reliability pass over the interactive surfaces: panels always close, settings toggles repaint immediately, icon labels no longer render as tofu, imported wallpapers are cropped and pruned, and filters no longer misclassify system TV apps.',
    groups: [
      {
        kind: 'Fixed',
        items: [
          'A settings or long-press panel now always dismisses. A panel whose caller passed no dismiss action used to fade out but leave its invisible scrim attached, swallowing every key press.',
          'Settings rows repaint immediately after a toggle: the check mark and accent colour are read back from the live preference instead of being painted once when the panel opened.',
          'Icon and text share a label again. Only the private-use glyph codepoints use the bundled symbol font and the words stay in Inter, so headings and chips no longer show tofu boxes.',
          'Typing in search no longer replays the card entrance animation on every keystroke.',
          'Imported wallpapers are center-cropped to the screen aspect when drawn, and each import deletes the previous JPEG. Switching back to a built-in wallpaper removes the imported file.',
          'The resume badge no longer points at whatever survived the current search or category filter; the target is resolved against every installed app.',
          'Reordering a pinned favourite no longer drops pins whose app is temporarily uninstalled, and it swaps slots so a hidden pin keeps its position.',
          'The Media category no longer swallows the system TV settings and the remote; hints match the package name only.',
          'The resume badge pulse runs a fixed number of breaths and stops on pause, instead of invalidating the view forever.',
          'Hero decoration repaints after a theme change, and the selected category chip keeps its fill when the cursor moves away.',
          'A package install or update no longer steals focus out of the search box, and pressing Home again only rescans when something changed.',
          'The clock keeps the system’s own 24-hour pattern, so locales that use a separator such as “HH.mm” still look native.',
          'The native search library degrades to the Kotlin fallback on any load failure, and labels containing tabs or newlines no longer corrupt the JNI payload.',
          'Release builds use the real bundled Inter 700 and 300 weights instead of synthesised bold and a 400 substitute.',
        ],
      },
      {
        kind: 'Changed',
        items: [
          'Renders triggered from the settings panel, from launching an app, or from a palette change skip the grid entrance animation.',
          'Dead code and one unused string were removed, and the docs describe the custom Dialog panels rather than the platform AlertDialog.',
        ],
      },
    ],
  },
  {
    version: '1.1.1',
    date: '2026-11-25',
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
