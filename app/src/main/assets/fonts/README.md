# Bundled fonts

All fonts are subsetted for BlueLauncher's UI and bundled as TTF assets.

| File | Source | License |
| --- | --- | --- |
| `inter_300.ttf`, `inter_400.ttf`, `inter_700.ttf` | [Inter](https://fonts.google.com/specimen/Inter) (Google Fonts), subsetted to Latin + typographic punctuation | [SIL OFL 1.1](INTER-OFL.txt) |
| `blue_icons.ttf` | [Material Symbols Rounded](https://fonts.google.com/icons) (Google Fonts), subsetted to the 25 glyphs used by the UI; variable axes (FILL/GRAD/opsz/wght) preserved | [Apache 2.0](MATERIAL-SYMBOLS-LICENSE.txt) |

Subsetting was done with `pyftsubset` (fonttools). To add a new icon glyph,
append its codepoint from the Material Symbols codepoints file to the
subset range and regenerate `blue_icons.ttf`.