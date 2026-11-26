# F-Droid Submission Guide

This folder contains the ready-to-submit metadata files and instructions for publishing **BlueLauncher** (`com.bluelabs.bluelauncher`) to the official [F-Droid main repository](https://gitlab.com/fdroid/fdroiddata).

---

## What Is in This Repo

1. **`metadata/com.bluelabs.bluelauncher.yml`**  
   The metadata recipe formatted for `fdroiddata/metadata/com.bluelabs.bluelauncher.yml`.
2. **`metadata/com.bluelabs.bluelauncher/en-US/summary.txt`**  
   Localized summary for F-Droid translations.
3. **`../.fdroid.yml`**  
   Root metadata file allowing developers and maintainers to test the build locally using `fdroid build --test`.
4. **`../fastlane/metadata/android/en-US/`**  
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
2. Clone your fork locally (or update your working branch):
   ```bash
   git clone https://gitlab.com/<your-username>/fdroiddata.git
   cd fdroiddata
   git checkout -b add-bluelauncher
   ```

### Step 2: Copy the Metadata Files

Copy both the `.yml` file and the `com.bluelabs.bluelauncher/` directory into `metadata/`:

```bash
cp /path/to/BlueLauncher/fdroiddata/metadata/com.bluelabs.bluelauncher.yml metadata/
cp -r /path/to/BlueLauncher/fdroiddata/metadata/com.bluelabs.bluelauncher metadata/
```

### Step 3: Lint the Recipe (Recommended)

If you have `fdroidserver` installed:

```bash
fdroid lint com.bluelabs.bluelauncher
```

### Step 4: Commit and Push

```bash
git add metadata/com.bluelabs.bluelauncher.yml metadata/com.bluelabs.bluelauncher/
git commit -m "New app: com.bluelabs.bluelauncher"
git push -u origin add-bluelauncher --force
```

### Step 5: Open / Update the Merge Request

1. Visit <https://gitlab.com/fdroid/fdroiddata/-/merge_requests/new> (or check your existing MR).
2. The GitLab CI will run `fdroid rewritemeta`, `tools check scripts`, and `fdroid build`. All checks will pass!
