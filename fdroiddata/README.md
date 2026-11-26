# F-Droid Submission Guide

This folder contains the ready-to-submit metadata file and instructions for publishing **BlueLauncher** (`com.bluelabs.bluelauncher`) to the official [F-Droid main repository](https://gitlab.com/fdroid/fdroiddata).

---

## What Is in This Repo

1. **`com.bluelabs.bluelauncher.yml`**  
   The metadata recipe formatted for `fdroiddata/metadata/com.bluelabs.bluelauncher.yml`.
2. **`../.fdroid.yml`**  
   Root metadata file allowing developers and maintainers to test the build locally using `fdroid build --test`.
3. **`../fastlane/metadata/android/en-US/`**  
   Complete store listing assets:
   - `title.txt` — App name (≤ 50 chars)
   - `short_description.txt` — Summary (≤ 80 chars)
   - `full_description.txt` — Long description with allowed HTML (≤ 4000 chars)
   - `changelogs/3.txt` — What's new for version code 3 / 1.1.1 (≤ 500 chars)
   - `images/icon.png` — 512×512 square store icon
   - `images/featureGraphic.png` — 1280×640 feature graphic
   - `images/tvScreenshots/` — 1920×1080 Android TV screenshots (1.png to 4.png)

---

## How to Submit to F-Droid (Step-by-Step)

### Step 1: Fork and Clone `fdroiddata`

1. Go to <https://gitlab.com/fdroid/fdroiddata> and click **Fork**.
2. Clone your fork locally:
   ```bash
   git clone https://gitlab.com/<your-username>/fdroiddata.git
   cd fdroiddata
   git checkout -b add-bluelauncher
   ```

### Step 2: Copy the Metadata File

Copy `com.bluelabs.bluelauncher.yml` into `metadata/`:

```bash
cp /path/to/BlueLauncher/fdroiddata/com.bluelabs.bluelauncher.yml metadata/
```

### Step 3: Lint the Recipe (Recommended)

If you have `fdroidserver` installed (Linux / WSL / Docker):

```bash
fdroid lint com.bluelabs.bluelauncher
```

It should pass without warnings or errors.

To run a test build locally inside the official Debian build environment via Docker:

```bash
docker run --rm -v $(pwd):/build:rw registry.gitlab.com/fdroid/ci-images-client:latest \
    bash -c "cd /build && fdroid lint com.bluelabs.bluelauncher"
```

### Step 4: Commit and Push

```bash
git add metadata/com.bluelabs.bluelauncher.yml
git commit -m "New app: com.bluelabs.bluelauncher"
git push -u origin add-bluelauncher
```

### Step 5: Open a Merge Request

1. Visit <https://gitlab.com/fdroid/fdroiddata/-/merge_requests/new>.
2. Set source branch to your `add-bluelauncher` branch and target to `fdroiddata:master`.
3. Fill in the standard F-Droid MR template:
   - **Application Name**: BlueLauncher
   - **Application ID**: `com.bluelabs.bluelauncher`
   - **License**: GPL-3.0-only (brand assets CC BY 4.0)
   - **Source**: `https://github.com/vryptt-ctrl/BlueLauncher`
   - Note that Rust 1.100.0 is installed in `prebuild` via checksum-pinned `rustup-init`.
4. Submit the Merge Request. The automated GitLab CI will run `fdroid lint` and attempt a test build on the F-Droid build server.

---

## Future Releases (When Tagging 1.1.2 / 1.2.0+)

1. In BlueLauncher:
   - Add new changelog under `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
   - Update `app/build.gradle.kts` (`versionCode`, `versionName`).
   - Tag the release (`vX.Y.Z`) and push.
2. Because `UpdateCheckMode: Tags` and `AutoUpdateMode: Version` are configured, F-Droid's checkupdates bot will automatically detect new tags and submit new builds to `fdroiddata`.
3. Once the first release containing `fastlane/` is built on F-Droid, you can file a quick MR on `fdroiddata` to delete the duplicate `Summary:` and `Description:` lines from `metadata/com.bluelabs.bluelauncher.yml` so F-Droid pulls them directly from the repo's fastlane files.
