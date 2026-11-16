# Lumina Photo Studio

**Lumina Photo** is a 100% on-device Android photo editor and portrait retouching studio built with Kotlin and Jetpack Compose (Material 3). Every edit, skin analysis, and background bokeh effect runs locally on your phone—no images or facial metrics are ever uploaded to external servers.

**Current Version:** `v2.8.0` (Build `14`)  
**Min SDK:** Android 7.0 (API 24) · **Target / Compile SDK:** Android 16 (API 36)

---

## Features

### Portrait Retouching Studio
- **Gaussian Mask Skin Smoothing:** Feathered localized skin smoothing with a **Detail** (`0%–75%`) texture-preservation slider and 1-tap **Auto Smooth**.
- **Precision Portrait Brushes & Restore Brush:** Teeth Whiten, Eye Pop, directional **Sculpt & Warp**, Detail Sharpen, Highlighter Glow, adjustable **S / M / L** brush presets, and an interactive **Restore** brush to paint pixels cleanly back to the original photo.

### Background Blur & Optical Bokeh
- **Background Blur (`Blur BG`):** Non-destructive two-pass optical bokeh under **Adjust → Effects** (plus a quick **Blur BG** shortcut in **Retouch**).
- **Interactive Tap-to-Focus:** Tap anywhere on the photo to move the sharp focus zone, adjust **Focus Size (`15%–70%`)**, and toggle **Protect Subject** to keep detected portrait subjects crisp.

### Color Grading, Tone & Filters
- **20+ Filters & Live CameraX Viewfinder:** Preview film, portrait, monochrome, and cinematic looks live in the camera before capturing.
- **Adjustments & 1-Tap Auto Enhance:** Exposure, Contrast, Highlights, Shadows, Whites, Blacks, Warmth, Tint, Saturation, Vibrance, Clarity, Sharpness, Soft Glow, Vignette, Film Grain, and Film Fade.
- **Tone Curves & HSL:** Interactive RGB / Red / Green / Blue curve editor with live histogram underlay, plus 8-channel selective HSL (`Red` through `Magenta`).

### Studio Workspace & Export
- **Precision Canvas Zoom (`1.0x–6.0x`):** Quick `+` / `-` / `1.0x` pill controls, double-tap focal zoom, and two-finger pinch/pan right inside Retouch mode.
- **Interactive Crop & Studio Frames:** Draggable canvas crop handles, locked aspect ratios (`1:1`, `3:4`, `4:5`, `2:3`, `16:9`, `9:16`), Rule-of-Thirds overlay, and Classic White, Polaroid, Cinema, and Obsidian frames.
- **Compare & Histograms:** Split Before/After slider, Hold-to-Compare original preview, and live RGB + Luminance Histogram HUD.
- **Export & Bilingual UI:** Full-resolution JPEG/PNG export to `Pictures/Lumina`, share sheet integration, Undo/Redo history, and full **English** + **Khmer (`ភាសាខ្មែរ`)** localization.

---

## Tech Stack

- **Language & UI:** Kotlin 2.2, Jetpack Compose, Material Design 3
- **Camera:** AndroidX CameraX (`camera-core`, `camera2`, `lifecycle`, `view`)
- **Image Processing:** Native Android `Bitmap` / `ColorMatrix` / custom multi-pass DSP (`BitmapProcessor`)
- **Networking & Updates:** OkHttp + Moshi (`AppUpdateChecker`)
- **Testing:** JUnit 4 & Robolectric

---

## Building Locally

### Prerequisites
- **JDK 21**
- **Android SDK 36** (`platforms;android-36.1`, `build-tools;36.0.0`)
- **Gradle 9.3.1** (or system `gradle` CLI)

### Commands
```bash
# Run unit & Robolectric tests
gradle :app:testDebugUnitTest

# Build debug APK
gradle :app:assembleDebug
```
The generated APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

---

## GitHub Actions APK Releases & In-App Updates

This repository includes an automated release workflow (`.github/workflows/release-apk.yml`) triggered by pushing a `v*` tag (e.g. `v2.8.0`) or manually from **Actions → Release Lumina Photo APK → Run workflow**.

### 1. Consistent Signing Key (for In-Place APK Updates)
To allow users to install new APK releases directly over older versions without uninstalling:
1. Encode your keystore (`debug.keystore.base64`) as a single-line base64 string.
2. In your GitHub repo, open **Settings → Secrets and variables → Actions → New repository secret**.
3. Create a secret named **`DEBUG_KEYSTORE_BASE64`** and paste the base64 string.

### 2. Customizing Release Name / Tag
When running **Release Lumina Photo APK** manually via **Run workflow**, you can set:
- **`tag`:** Release tag (default `v2.8.0`)
- **`apk_name`:** Optional APK asset filename (default `LuminaPhoto.apk`)
- **`release_title`:** Optional GitHub Release title

### 3. In-App Update Checker URL
In the app, open **Settings → Check for Updates → Update Feed URL** and enter:
```text
https://github.com/jobicam/Lumina-photo-studio
```
*(Requires the repository to be Public with at least one published Release.)*
