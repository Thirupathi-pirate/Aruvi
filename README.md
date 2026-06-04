# Aruvi

Stream your Telegram media on Android phone, tablet, and TV.

<img src="img/full.png" width="100%" alt="Aruvi Screenshots"/>

## Features

- Browse and search your media library
- Stream video and audio with hardware-accelerated playback
- Picture-in-Picture mode (mobile)
- Audio-only background playback
- Download files for offline playback
- Android TV leanback UI optimized for remote control
- Continue watching across devices

## Setup

The app is pre-configured to connect to `https://lavender7736-teleplay-backend.hf.space`.

1. Open the app
2. A login code appears on screen
3. Send `/login <code>` to [@Aaruvi_movie_bot](https://t.me/Aaruvi_movie_bot) on Telegram
4. The app logs in automatically

> **Note:** You need a running [TelePlay Backend](https://github.com/Thirupathi-pirate/teleplay-hf) instance.

## Building

```bash
git clone https://github.com/Thirupathi-pirate/Aruvi.git
cd Aruvi

# Create local.properties with signing config (optional for debug builds)
cp local.properties.example local.properties

# Build debug APK
./gradlew assembleDebug

# Build universal release APK
./gradlew assembleRelease
```

APKs are output to `app/build/outputs/apk/`.

## Tech Stack

- **Kotlin** + **Jetpack Compose** (mobile & TV)
- **ExoPlayer (Media3)** for playback
- **Hilt** for dependency injection
- **Retrofit** + **OkHttp** for networking
- **Coil** for image loading
- **DataStore** for preferences

## License

MIT
