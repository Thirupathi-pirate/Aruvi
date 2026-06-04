# Aruvi — Agent Guide

## Build
```bash
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # signed, minified release APK
```
APKs → `app/build/outputs/apk/`

## Project Map
- **Single module** — namespace `com.aruvi.tir`, minSdk 21, targetSdk 34
- **`data/`** — API (Retrofit), models (Gson), repos (DataStore) — **do not modify**, these are from upstream TelePlay and are pre-tested
- **`di/`**, **`download/`**, **`service/`**, **`TelePlayApp.kt`** — also upstream, do not modify
- **`ui/`** — TV screens (leanback Compose)
- **`ui/mobile/`** — phone/tablet screens (standard Compose)
- **`ui/player/`** — shared player ViewModel used by both TV and mobile

## Key Conventions
- **Never touch backend files** (`data/`, `di/`, `download/`, `service/`, `TelePlayApp.kt`) — they are tested upstream code
- **Signing** — env vars (`RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`) or `local.properties`
- **Server URL** — defaults to `https://lavender7736-teleplay-backend.hf.space`, overridable via `local.properties` key `TELEGRAM_TV_SERVER_URL` or in-app settings
- **R8 full mode** disabled in `gradle.properties` (breaks Retrofit/Gson)
- **ZXing 3.5.3** added for QR code login

## Dev Setup
```bash
cp local.properties.example local.properties
# Edit sdk.dir and optionally TELEGRAM_TV_SERVER_URL
```

## Tech Stack
Kotlin 1.9.22 · AGP 8.2.2 · Gradle 8.5 · Compose BOM 2024.02 · Media3 1.2.1 · Hilt 2.50 · Retrofit 2.9 · DataStore

## Features (kept from Aruvi, not in upstream TelePlay)
- **QR login** — encodes `https://t.me/$bot?start=$code` via ZXing, displays QR on TV
- **Quality control** — `preferredQuality` in player settings (Auto/1080p/720p/480p/360p), applies `setMaxVideoSize` on ExoPlayer
- **Open in external player** — `Intent.ACTION_VIEW` with `video/*` in both TV and mobile players
- **Double-tap seek** — mobile only, left half = rewind 10s, right half = forward 10s

## ABI Splits
`armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64` + universal APK
