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
- Navigable folder tree for moving files into subfolders

## Setup

The app is pre-configured to connect to `https://lavender7736-teleplay-backend.hf.space`.

1. Open the app
2. A login code appears on screen
3. Send `/login <code>` to [@Aaruvi_movie_bot](https://t.me/Aaruvi_movie_bot) on Telegram
4. The app logs in automatically

> **Note:** You need a running [TelePlay Backend](https://github.com/Thirupathi-pirate/teleplay-hf) instance.

## Building

Two product flavors: `mobile` (phone/tablet) and `tv` (Android TV).

```bash
git clone https://github.com/Thirupathi-pirate/Aruvi.git
cd Aruvi

cp local.properties.example local.properties
# Edit sdk.dir and optionally TELEGRAM_TV_SERVER_URL

# Fast compile check
GRADLE_USER_HOME=/tmp/.gradle ./gradlew compileMobileDebugKotlin
GRADLE_USER_HOME=/tmp/.gradle ./gradlew compileTvDebugKotlin

# Build debug APK
GRADLE_USER_HOME=/tmp/.gradle ./gradlew assembleMobileDebug
GRADLE_USER_HOME=/tmp/.gradle ./gradlew assembleTvDebug

# Build release APK (signed, R8 minified)
GRADLE_USER_HOME=/tmp/.gradle ./gradlew assembleMobileRelease
GRADLE_USER_HOME=/tmp/.gradle ./gradlew assembleTvRelease
```

APKs are output to `app/build/outputs/apk/{mobile,tv}/release/`. Each flavor produces per-ABI APKs (`armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`) plus a universal APK.

## Releases

Pre-built APKs are available on the [releases page](https://github.com/Thirupathi-pirate/Aruvi/releases).

## Tech Stack

- **Kotlin** + **Jetpack Compose** (mobile & TV)
- **ExoPlayer (Media3)** for playback
- **Hilt** for dependency injection
- **Retrofit** + **OkHttp** for networking
- **Coil** for image loading
- **DataStore** for preferences

## License

MIT
