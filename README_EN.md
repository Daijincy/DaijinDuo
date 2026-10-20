# DaijinDuo

> **Recreating the iPhone DUO tilt effect with the Android gyroscope.**

<p align="center">
  <a href="README.md"><strong>简体中文</strong></a> · <strong>English</strong>
</p>

Treat your phone's home-screen screenshot as a *breathing sheet of glass* — flat when held level, folding 90° along a vertical hinge as you tilt in any direction, with **perspective foreshortening, depth-of-field blur (Gaussian pyramid), hinge dark-edge gradient and glass reflection** — a faithful recreation of the [solotilt.com](https://solotilt.com/) iPhone DUO fold effect.

**v0.3 (official release)** · Package `com.daijinduo.github`

## ✨ Features

- **Gyroscope driven**: native accelerometer feeds the WebView render core through a JS bridge; the mapping math matches the web version line-for-line (`sign=-1` + exponential smoothing)
- **WebView / WebGL2 core**: rendering is fully embedded in the browser engine, avoiding native GLES compatibility issues — works on virtually any device
- **🎛 In-fullscreen effect tuning**: **double-tap** the fullscreen view to pop up the tuning panel — **blur strength** · **gyroscope sensitivity** · **blur trigger angle** (persisted locally); a first-run hint teaches the gesture and can be set to "don't show again"
- **Retro-minimal UI**: bundled Source Han Serif (subset) · pure black & white · white-outlined brand logo
- **Liquid Glass bottom bar**: iOS 26 Liquid Glass style floating tab bar (real background blur via Haze · top gradient highlight · bouncy liquid selection pill)
- **Two pages**: `Duo` (main feature) / `Show` (coming soon)
- **Tap 3 times to exit** full-screen effect
- **GitHub Actions cloud build** with automatic APK release

## 🖼 Interaction flow

1. Open the app: `Duo` page (retro-minimal home + brand logo + fold preview)
2. **Gallery** (black button, white outline) → request gallery permission → browse recent images in a grid
3. **Files** (white button, dark text) → pick via system Photo Picker
4. Tap an image → full-screen immersive fold effect starts (first entry shows a "double-tap to tune" hint, can be set to don't-show-again)
5. Tilt the phone: fold amount grows with tilt angle (0–90° full fold), direction follows tilt
6. **Double-tap the screen** → pop up the tuning panel (blur strength / gyroscope sensitivity / blur trigger angle; applies live & persists); tap the dim or ✕ to close
7. Tap the screen 3 times (within 800 ms) → back to home
8. Switch to the `Show` page (coming soon) from the glass bottom bar

## 📁 Project layout

```
app/src/main/
├── assets/daijinduo/index.html     # WebView render core (WebGL2 fold shaders, line-by-line port)
├── java/com/daijinduo/github/
│   ├── MainActivity.kt             # Two-page skeleton (Duo/Show) + Liquid Glass bar
│   ├── WebViewActivity.kt          # Full-screen page: HTML injection + JS bridge + native accelerometer + double-tap tuning
│   ├── SettingsStore.kt            # Effect settings persistence (SharedPreferences, incl. "don't show again")
│   ├── ui/
│   │   ├── Theme.kt                # Noto Serif CJK · retro-minimal palette
│   │   ├── HomeScreen.kt           # Duo home (logo + entry buttons)
│   │   ├── FoldAdjustOverlay.kt    # Fullscreen double-tap tuning panel + first-run hint banner
│   │   ├── GalleryScreen.kt        # Gallery permission + image grid
│   │   ├── ShowPage.kt             # Show page (coming soon)
│   │   └── LiquidGlassBar.kt       # Liquid Glass floating bar (Haze)
│   └── data/GalleryLoader.kt       # MediaStore queries & thumbnails
ref/                                # solotilt.com reverse-engineering notes
```

## ☁️ Cloud build (GitHub Actions)

Pushing to `main` triggers a build automatically (or run `workflow_dispatch` manually):

- JDK 17 + Android SDK 35
- `gradle :app:assembleRelease` (debug-signed, installable directly)
- Artifact published to **GitHub Release**: `DaijinDuo-v0.3.apk`

## 📦 Version history

| Version | Notes |
|---|---|
| **v0.3** | Tuning **moved into the fullscreen view**: **double-tap** to open the panel (blur strength / gyroscope sensitivity / blur trigger angle); first-run hint teaches the gesture, "don't show again" option; home-page settings card removed · signing unchanged · package `com.daijinduo.github` |
| v0.2 | Added effect settings: blur strength / gyroscope sensitivity / blur trigger angle (home-page sliders, persisted locally, injected into the render core) · signing unchanged · package `com.daijinduo.github` |
| v0.1.1 | Bundled Source Han Serif (subset, consistent rendering on all devices) · pure black & white theme · package `com.daijinduo.github` |
| v0.1ZS | First official release: retro-minimal UI · Noto Serif CJK · white-outlined logo · Liquid Glass bar · two pages (Duo/Show) · package `com.daijinduo.github` (versioning restarted) |

> Note: dev iterations v1.0.0 ~ v1.1.2 were removed; official versioning starts at **v0.1ZS**.

## 🛠 Local build

```bash
./gradlew :app:assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

## ⚙️ Effect tuning (v0.3 via double-tap in fullscreen; the rest match the web version)

| Parameter | Default | Range | Meaning |
|---|---|---|---|
| **Blur strength** 🎛 | 0.0315 | 0 – 0.08 | max blur (fraction of image width) |
| **Gyroscope sensitivity** 🎛 | 1.0× | 0.5× – 2.0× | response gain of fold vs. tilt angle |
| **Blur trigger angle** 🎛 | 90° | 30° – 120° | tilt angle for full blur |
| fullAngle (legacy) | 90° | — | same, overridden by trigger angle |
| anglePower | 0.5 | — | angle response exponent |
| fullDistance | 0.7 | — | distance from hinge for full blur |
| distancePower | 1.45 | — | distance response exponent |
| edgeBlur | 0.18 | — | blur at the hinge |
| gradientOpacity | 0.7 | — | dark-edge opacity |

> 🎛 = opened in the fullscreen view via double-tap, applies live and persists locally (SharedPreferences), injected into the WebView render core; checking "don't show again" stops the hint on later entries.
