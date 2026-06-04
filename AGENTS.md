# Aruvi — Agent Guide

## Build & Debug Loop

```bash
# ── Fast compile check (debug session only) ──
./gradlew compileTvDebugKotlin          # TV Kotlin only (fastest, ~30s)
./gradlew compileMobileDebugKotlin      # Mobile Kotlin only
./gradlew kaptTvDebugKotlin compileTvDebugKotlin  # with Hilt annotation processing
./gradlew kaptMobileDebugKotlin compileMobileDebugKotlin

# ── Full APK build (only for final verification) ──
GRADLE_USER_HOME=/tmp/.gradle ./gradlew assembleTvDebug
GRADLE_USER_HOME=/tmp/.gradle ./gradlew assembleMobileDebug
GRADLE_USER_HOME=/tmp/.gradle ./gradlew assembleTvRelease
GRADLE_USER_HOME=/tmp/.gradle ./gradlew assembleMobileRelease
```
- **Debug session** — only run `compile*Kotlin`. Never build full APK during development.
- **Release APK** uses R8 minification, takes ~8min. Only at the very end.
- **`GRADLE_USER_HOME=/tmp/.gradle`** — keeps build cache in /tmp, avoids filling disk.

## Product Flavors
| Flavor | minSdk | targetSdk | compileSdk | Uses Feature |
|---|---|---|---|---|
| **mobile** | 29 | 36 | 36 | `touchscreen required="true"` |
| **tv** | 29 | 30 | 36 | `leanback required="true"` |

## Project Map
- **Product flavors** — `mobile` (phone), `tv` (Android TV), share all `.kt` source files
- **`data/`** — API (Retrofit), models (Gson), repos (DataStore) — **NEVER modify**, upstream TelePlay
- **`di/`**, **`download/`**, **`service/`**, **`TelePlayApp.kt`** — also upstream, **NEVER modify**
- **`ui/`** — TV screens (leanback Compose) — safe to modify
- **`ui/mobile/`** — phone/tablet screens (standard Compose) — safe to modify
- **`ui/player/`** — shared player ViewModel used by both TV and mobile

## Critical Rules
1. **Never touch backend files** (`data/`, `di/`, `download/`, `service/`, `TelePlayApp.kt`) — upstream TelePlay code. Changes break upstream sync.
2. **Debug session = compile check only.** No full APK builds.
3. **TV is simpler than mobile** — no server URL config, no Telegram button, no unnecessary features on TV.
4. **Use /tmp for Gradle cache** — `GRADLE_USER_HOME=/tmp/.gradle`

## TV UI Conventions
- **Animation** — use `Modifier.graphicsLayer { scaleX; scaleY }` NOT `Modifier.scale()`. The latter triggers layout pass and causes jank on leanback hardware.
- **No bouncy springs** — use `DampingRatioNoBouncy` + `StiffnessHigh`.
- **Login screen** — code and QR code take equal halves (`weight(1f)` each), QR fills its half.
- **Player controls** — Speed/Resize/External Player go inside the Settings panel (slide-in from right), NOT in the top bar.
- **Settings button** — always visible in the top bar (not conditional on audio/subtitle tracks).
- **DPAD_UP during playback** — opens the Settings panel.

## Key Conventions
- **Signing** — env vars (`RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`) or `local.properties`
- **Server URL** — defaults to `https://lavender7736-teleplay-backend.hf.space`, overridable via `local.properties` key `TELEGRAM_TV_SERVER_URL` or in-app settings
- **R8 full mode** disabled in `gradle.properties` (breaks Retrofit/Gson)
- **ZXing 3.5.3** added for QR code login
- **gradle.properties** — configured for parallel + caching + 8g heap

## Dev Setup
```bash
cp local.properties.example local.properties
# Edit sdk.dir and optionally TELEGRAM_TV_SERVER_URL
```

## Tech Stack
Kotlin 1.9.22 · AGP 8.5.2 · Gradle 8.7 · Compose BOM 2024.02 · Media3 1.2.1 · Hilt 2.50 · Retrofit 2.9 · DataStore

## Features (kept from Aruvi, not in upstream TelePlay)
- **QR login** — encodes `https://t.me/$bot?start=$code` via ZXing, displays QR on TV
- **Quality control** — `preferredQuality` in player settings (Auto/1080p/720p/480p/360p), applies `setMaxVideoSize` on ExoPlayer
- **Open in external player** — `Intent.ACTION_VIEW` with `video/*` in both TV and mobile players
- **Double-tap seek** — mobile only, left half = rewind 10s, right half = forward 10s

## ABI Splits
`armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64` + universal APK
