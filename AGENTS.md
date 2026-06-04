# Aruvi — Agent Guide

## Project Overview
Aruvi is a **Telegram-based file browser and media player** for Android (mobile + TV). Users log in via a Telegram bot, browse files/folders stored on their Telegram account, and stream videos/audio directly — no local storage needed.

It is a fork of `subinps/TelePlay` (package `com.telegramtv`), modified extensively for a better mobile app experience and a dedicated Android TV leanback UI. The backend runs on Hugging Face Spaces — the app just connects to it via Retrofit API.

### Architecture
- **Login**: QR code or bot token → get JWT token → auth header on all requests
- **File browsing**: folders tree hierarchy, each folder can contain files + subfolders
- **Media playback**: ExoPlayer (Media3 1.2.1) with DASH/HLS/MP4 support, quality presets, external player, double-tap seek (mobile)
- **Two products from one codebase**: `mobile` (phone/tablet, touchscreen) and `tv` (Android TV, leanback D-pad) via Gradle product flavors

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
- **Release APK** uses R8 minification, takes ~6-8min. Only at the very end.
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
- **Animation** — use `Modifier.graphicsLayer { scaleX; scaleY; alpha }` NOT `Modifier.scale()`. The latter triggers layout pass and causes jank on leanback hardware.
- **Alpha dim (brightness lift)** — unfocused 0.88f → focused 1.0f on cards for visual pop without `RenderEffect` (API 31+).
- **Shadow elevation** — card shadow lift from 12→16dp on focus; spot color alpha 0.25→0.4.
- **No bouncy springs** — use `DampingRatioNoBouncy` + `StiffnessHigh`.
- **Login screen** — code and QR code take equal halves (`weight(1f)` each), QR fills its half. Shows instruction: "Open Telegram, send /login {code} to @{botUsername}" using dynamic `botUsername` from `uiState`.
- **Player controls** — Speed/Resize/External Player go inside the Settings panel (slide-in from right), NOT in the top bar.
- **Settings button** — always visible in the top bar (not conditional on audio/subtitle tracks).
- **DPAD_UP during playback** — opens the Settings panel.
- **ControlIconButton scale** — 1.0→1.12 via `graphicsLayer` with stiff spring (`DampingRatioNoBouncy` + `StiffnessHigh`), no bouncy.
- **PlayPauseButton shadow** — ambient alpha 0.6f for stronger lift effect.
- **Auto-play-next (audio)** — `PlayerViewModel` fetches folder audio list on load, advances on `STATE_ENDED`.

## Mobile UI Conventions
- **Move Picker Dialog** — `MovePickerDialog` in `MobileComponents.kt:118` loads full folder tree via `FoldersRepository.getFolderTree()`. Users can navigate into subfolders with path breadcrumb, ".. (Up)" back, and "Root Directory" shortcut. Folders with children show `>` indicator — tap to navigate in. Leaf folder tap immediately confirms the move. The dialog self-manages navigation state (no ViewModel involvement beyond providing the `loadFolderTree` suspend lambda).
- **Subtitles default off** — `PlayerViewModel.kt` init block sets `C.TRACK_TYPE_TEXT` disabled. Default `subtitlesEnabled` changed from `true` to `false`.

## Key Conventions
- **Signing** — env vars (`RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`) or `local.properties`
- **Server URL** — defaults to `https://lavender7736-teleplay-backend.hf.space`, overridable via `local.properties` key `TELEGRAM_TV_SERVER_URL` or in-app settings
- **R8 full mode** disabled in `gradle.properties` (breaks Retrofit/Gson)
- **ZXing 3.5.3** added for QR code login
- **gradle.properties** — configured for parallel + caching + 8g heap

## CI/CD: Aruvi-workflow
Release builds are handled by a separate repo: `Thirupathi-pirate/Aruvi-workflow`. The `Aruvi` dev repo has NO workflow files — all CI/CD lives in `Aruvi-workflow`.

### How to release
```bash
# After committing changes to Aruvi:
git push origin main          # push to Aruvi (your dev repo)
git push workflow main        # push to Aruvi-workflow → triggers build
# If rejected (Aruvi-workflow has its own workflow commits), force push then restore workflow:
git push workflow main --force
# Then restore .github/workflows/sync-and-release.yml from backup
```

### The workflow (sync-and-release.yml)
1. Fetches latest code from `Aruvi` repo
2. Optionally syncs backend from upstream `subinps/TelePlay` (if manually dispatched with checkbox)
3. Auto-bumps version (reads latest tag, increments patch)
4. Sets up swap space (4GB) + Gradle opts (`-Xmx3g -XX:MaxMetaspaceSize=512m`) for R8 OOM prevention
5. Builds TV + Mobile release APKs
6. Creates tag + GitHub release with APKs
7. Pushes tag to `Aruvi` repo

### CI Learnings (R8 OOM on GitHub Actions)
- GitHub Actions runner has limited memory (~7GB)
- R8 requires more memory than available — fixed with:
  - 4GB swap space (created in workflow)
  - `-Dorg.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=512m` in `gradle.properties`
- First workflow run is slow (~15min cold Gradle cache), subsequent runs ~5min
- Debug compile locally is much faster (~30-60s)

### How to trigger upstream sync
Go to `Aruvi-workflow` → Actions → "Sync & Release" → "Run workflow" → check **"Sync backend from subinps/TelePlay"** → enter upstream version tag → Run.

### Remote configured
```bash
git remote add workflow https://github.com/Thirupathi-pirate/Aruvi-workflow.git
```

### Secrets (on Aruvi-workflow repo)
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_PASSWORD`
- `GH_PAT` — for checking out Aruvi and creating releases (needed because `GITHUB_TOKEN` can't create releases for new tags)

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
- **Auto-play-next (audio)** — fetches all audio files in same folder, advances sequentially on track end
- **Move Picker with folder tree navigation** — `MovePickerDialog` loads `getFolderTree()` and lets users drill into subfolders to place files/folders anywhere in hierarchy
- **Subtitles default off** — text tracks disabled at player init; user must manually enable

## ABI Splits
`armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64` + universal APK

## Last Working Build
- **Commit**: `b8db22c` — "feat: navigable folder tree in MovePickerDialog + v3.0.0"
- **Tag**: `v3.0.0`
- **Release**: https://github.com/Thirupathi-pirate/Aruvi/releases/tag/v3.0.0
- **Both flavors compiled & released**: TV + mobile arm64-v8a and universal (11MB each)
