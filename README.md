# Lumina Photo Studio

**Lumina Photo** is a 100% on-device Android photo editor and portrait retouching studio built with Kotlin and Jetpack Compose (Material 3). Every edit, skin analysis, and background bokeh effect runs locally on your phone—no images or facial metrics are ever uploaded to external servers.

**Current Version:** `v2.7.0` (Build `13`)  
**Min SDK:** Android 7.0 (API 24) · **Target / Compile SDK:** Android 16 (API 36)

---

## Features

### Portrait Retouching & Blemish Removal
- **Automated Skin Blemish Detection:** Scans skin regions using YCbCr/HSV skin-tone detection and blob analysis, displaying interactive target rings to tap-to-heal individually or **Heal All** in one tap with adjustable sensitivity.
- **Gaussian Mask Skin Smoothing:** Feathered localized skin smoothing with a **Detail** (`0%–75%`) texture-preservation slider and 1-tap **Auto Smooth**.
- **Precision Portrait Brushes:** Teeth Whiten, Eye Pop, Sculpt & Warp, Detail Sharpen, Highlighter Glow, and adjustable Brush Size / Intensity.

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
- **Image Processing:** Native Android `Bitmap` / `ColorMatrix` / custom multi-pass DSP
- **Networking & Updates:** OkHttp + Moshi
- **Testing:** JUnit 4 & Robolectric

---

## Building Locally

### Prerequisites
- **JDK 21**
- **Android SDK 36** (`platforms;android-36.1`, `build-tools;36.0.0`)
- **Gradle 9.3.1**

### Commands
```bash
# Run unit & Robolectric tests
gradle :app:testDebugUnitTest

# Build debug APK
gradle :app:assembleDebug
