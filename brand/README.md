# BlueLauncher brand assets

Concept: **Focus Grid**. A 2×2 grid of app tiles, one lit up with a focus ring. It represents a D-pad-driven launcher.

## Colors

| Role | Hex |
| --- | --- |
| Navy (background) | `#0B1220` |
| Primary blue | `#2F7DFF` |
| Bright blue | `#5AA9FF` |
| Cyan (focus ring, glow) | `#7CE1FF` |
| Dim tile, light background | `#25406E` |
| Dim tile, dark background | `#3A5C94` |
| Secondary text on dark | `#7CC4FF` |

## Typography

The wordmark uses **Inter** (Bold for "Blue", Regular for "Launcher"). The SVG files contain live text and will fall back to Helvetica/Arial if Inter is not installed. All PNGs were rendered with Inter.

## Folder contents

```
svg/        vector sources (edit here)
  icon.svg                  icon with rounded corners
  icon-square.svg           full-square icon (Play Store, system masks)
  icon-round.svg            circular icon
  mark-color.svg            mark only, light background
  mark-color-on-dark.svg    mark only, dark background
  mark-white.svg / mark-dark.svg   single color
  logo-horizontal-light.svg / -dark.svg   mark + name
  banner-tv.svg             Android TV banner, 320×180
  social-preview.svg        GitHub preview image, 1280×640
png/        ready-to-use exports (icons 16–1024, logos, banner, favicon)
android/res/  drop-in for app/src/main/res
```

## Installing into the Android project

1. Copy the contents of `android/res/` into `app/src/main/res/` (merge the folders).
2. In `AndroidManifest.xml`, on `<application>`:

   ```xml
   android:icon="@mipmap/ic_launcher"
   android:roundIcon="@mipmap/ic_launcher_round"
   android:banner="@drawable/tv_banner"
   ```

3. `minSdk 23`: Android 7.x devices use the PNGs in `mipmap-*dpi`, Android 8+ uses the adaptive icon XML. Android 13+ automatically applies the monochrome layer for themed icons.
4. The TV banner must be 320×180 px in `drawable-xhdpi` and include the app name. This is already satisfied.

## Usage

- Keep clear space around the logo of at least the height of one small tile.
- Do not rotate, stretch, or recolor the lit tile. The focus ring always sits on the top-right tile.
- Minimum icon size: 24 px. Below that, use `mark-color.svg` without a background.
- The logo is original artwork, not derived from any other brand.

## License

The brand assets are licensed under **CC BY 4.0** — see [LICENSE-ASSETS.md](LICENSE-ASSETS.md) for full terms and attribution instructions. The program code remains under GPL-3.0; the two licenses are independent.
