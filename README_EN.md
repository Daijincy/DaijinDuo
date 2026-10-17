# DaijinDuo

> **Recreating the iPhone DUO tilt effect with the Android gyroscope.**

<p align="center">
  <a href="README.md"><strong>简体中文</strong></a> · <strong>English</strong>
</p>

Treat your phone's home-screen screenshot as a *breathing sheet of glass* — flat when held level, folding 90° along a vertical hinge as you tilt in any direction, with **perspective foreshortening, depth-of-field blur (Gaussian pyramid), hinge dark-edge gradient and glass reflection** — a faithful recreation of the [solotilt.com](https://solotilt.com/) iPhone DUO fold effect.

**v0.1.1 (official release)** · Package `com.daijinduo.github`

## ✨ Features

- **Gyroscope driven**: native accelerometer feeds the WebView render core through a JS bridge; the mapping math matches the web version line-for-line (`sign=-1` + exponential smoothing)
- **WebView / WebGL2 core**: rendering is fully embedded in the browser engine, avoiding native GLES compatibility issues — works on virtually any device
- **Retro-minimal UI**: bundled Source Han Serif (subset) · pure black & white · white-outlined brand logo
- **Liquid Glass bottom bar**: iOS 26 Liquid Glass style floating tab bar (real background blur via Haze · top gradient highlight · bouncy liquid selection pill)
- **Two pages**: `Duo` (main feature) / `Show` (coming soon)
- **Tap 3 times to exit** full-screen effect
- **GitHub Actions cloud build** with automatic APK release

## 🖼 Interaction flow

1. Open the app: `Duo` page (retro-minimal home + brand logo + fold preview)
2. **Gallery** (black button, white outline) → request gallery permission → browse recent images in a grid
3. **Files** (white button, dark text) → pick via system Photo Picker
4. Tap an image → full-screen immersive fold effect starts
5. Tilt the phone: fold amount grows with tilt angle (0–90° full fold), direction follows tilt
6. Tap the screen 3 times (within 800 ms) → back to home
7. Switch to the `Show` page (coming soon) from the glass bottom bar

## 📁 Project layout

```
app/src/main/
├── assets/daijinduo/index.html     # WebView render core (WebGL2 fold shaders, line-by-line port)
├── java/com/daijinduo/github/
│   ├── MainActivity.kt             # Two-page skeleton (Duo/Show) + Liquid Glass bar
│   ├── WebViewActivity.kt          # Full-screen page: HTML injection + JS bridge + native accelerometer
│   ├── ui/
│   │   ├── Theme.kt                # Noto Serif CJK · retro-minimal palette
│   │   ├── HomeScreen.kt           # Duo home (logo + left/right entry buttons)
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
- Artifact published to **GitHub Release**: `DaijinDuo-v0.1.1.apk`

## 📦 Version history

| Version | Notes |
|---|---|
| **v0.1.1** | Bundled Source Han Serif (subset, consistent rendering on all devices) · pure black & white theme · package `com.daijinduo.github` |
| v0.1ZS | First official release: retro-minimal UI · Noto Serif CJK · white-outlined logo · Liquid Glass bar · two pages (Duo/Show) · package `com.daijinduo.github` (versioning restarted) |

> Note: dev iterations v1.0.0 ~ v1.1.2 were removed; official versioning starts at **v0.1ZS**.

## 🛠 Local build

```bash
./gradlew :app:assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

## ⚙️ Effect parameters (same as the web version)

| Parameter | Value | Meaning |
|---|---|---|
| strength | 0.0315 | max blur (fraction of image width) |
| fullAngle | 90° | tilt angle for full blur |
| anglePower | 0.5 | angle response exponent |
| fullDistance | 0.7 | distance from hinge for full blur |
| distancePower | 1.45 | distance response exponent |
| edgeBlur | 0.18 | blur at the hinge |
| gradientOpacity | 0.7 | dark-edge opacity |

## 📜 License

For personal learning and experience only. The fold effect is reverse-engineered from [solotilt.com](https://solotilt.com/); all rights belong to the original author.
