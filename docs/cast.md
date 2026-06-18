# Chromecast Integration — Official Media3 CastPlayer

## Root Cause of Previous Crashes

The app crashed with `FATAL ERROR IN MAIN` when clicking play because:

1. **Two conflicting APIs mixed**: Old Google Cast SDK (`CastContext.getSharedInstance`, `SessionManagerListener`, `CastButtonFactory.setUpMediaRouteButton`) + new Media3 `CastPlayer`.
2. **`CastContext.getSharedInstance()`** fails on devices with incomplete/outdated Google Play Services (e.g., Qualcomm devices with KernelSU root).
3. **Reflection-based instantiation** (`Class.forName("androidx.media3.cast.CastPlayer")`) masked class-loading errors until runtime.
4. **`CastButtonFactory.setUpMediaRouteButton()`** via reflection triggers internal Cast SDK initialization that crashes if Play Services is broken.

## Official Media3 CastPlayer Integration

### Architecture

```
┌─────────────────────────────────────────────────┐
│  Manifest                                       │
│  ├── DefaultCastOptionsProvider (Media3 built-in)│
│  └── (no MediaTransferReceiver in 1.2.1)        │
├─────────────────────────────────────────────────┤
│  PlayerViewModel                                 │
│  ├── ExoPlayer (Hilt singleton)                  │
│  ├── CastContext.getSharedInstance() (lazy, safe) │
│  ├── CastPlayer(castContext) with try-catch      │
│  ├── Player.Listener (DeviceInfo tracking)       │
│  └── direct setMediaItem/prepare/play on cast   │
├─────────────────────────────────────────────────┤
│  MobilePlayerScreen                              │
│  └── MediaRouteButton (plain composable)         │
└─────────────────────────────────────────────────┘
```

### Key Differences: Old vs New

| Aspect | Old (BROKEN) | New (Official) |
|--------|-------------|----------------|
| Options provider | Custom `CastOptionsProvider.kt` | `DefaultCastOptionsProvider` (Media3 built-in) |
| CastPlayer init | `CastPlayer(castContext)` via reflection + Hilt DI | `CastPlayer(CastContext.getSharedInstance())` with full try-catch |
| Session management | Manual `SessionManagerListener` via `CastHelper` | Built-in (via `Player.Listener.onDeviceInfoChanged`) |
| MediaRouteButton | `CastButtonFactory.setUpMediaRouteButton()` via reflection | Plain `MediaRouteButton(ctx)` composable |
| CastContext | Hilt-injected singleton (crashes app at startup if Play Services broken) | Lazy init in ViewModel, fails gracefully (null castPlayer) |
| Manifest meta-data | Custom class name | `androidx.media3.cast.DefaultCastOptionsProvider` |

### Step 1: AndroidManifest.xml

```xml
<!-- REPLACE old custom provider with Media3 default -->
<meta-data
    android:name="com.google.android.gms.cast.framework.OPTIONS_PROVIDER_CLASS_NAME"
    android:value="androidx.media3.cast.DefaultCastOptionsProvider" />

<!-- ADD MediaTransferReceiver for system-level Cast discovery -->
<receiver
    android:name="androidx.media3.cast.MediaTransferReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MEDIA_BUTTON" />
    </intent-filter>
</receiver>
```

### Step 2: PlayerViewModel.kt — CastPlayer Creation

```kotlin
// media3-cast:1.2.1 uses CastPlayer(CastContext) — CastPlayer.Builder was added in 1.4.0
private val castPlayer: CastPlayer? = try {
    val castContext = CastContext.getSharedInstance(context)
    CastPlayer(castContext)
} catch (_: Throwable) {
    null  // Graceful fallback if Cast SDK unavailable
}
```

### Step 3: DeviceInfo Listener (Local vs Remote Detection)

```kotlin
private val castDeviceListener = object : Player.Listener {
    override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
        when (deviceInfo.playbackType) {
            DeviceInfo.PLAYBACK_TYPE_LOCAL -> {
                _uiState.value = _uiState.value.copy(isCasting = false)
            }
            DeviceInfo.PLAYBACK_TYPE_REMOTE -> {
                _uiState.value = _uiState.value.copy(isCasting = true)
            }
        }
    }
}

// Register in init block
castPlayer?.addListener(castDeviceListener)
```

### Step 4: MediaRouteButton (Compose)

```kotlin
@Composable
fun CastButton(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            MediaRouteButton(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(48.dp.toPx().toInt(), 48.dp.toPx().toInt())
            }
        },
        modifier = modifier.size(48.dp)
    )
}
```

### Step 5: ProGuard Rules

```proguard
# MediaRouteButton (from official Cast demo)
-keep class androidx.mediarouter.app.MediaRouteActionProvider { *; }

# Cast Framework (already present, keep as-is)
-keep class com.google.android.gms.cast.** { *; }
-keep class com.google.android.gms.cast.framework.** { *; }
```

### Step 6: Dependencies (no changes needed)

```kotlin
// Already correct in build.gradle.kts
implementation("androidx.media3:media3-cast:1.2.1")
implementation("com.google.android.gms:play-services-cast-framework:21.4.0")
implementation("androidx.mediarouter:mediarouter:1.6.0")
```

## Files to Delete

| File | Reason |
|------|--------|
| `di/CastModule.kt` | Hilt module providing CastContext — no longer needed |
| `di/CastOptionsProvider.kt` | Custom provider — replaced by DefaultCastOptionsProvider |
| `di/CastHelper.kt` | Reflection wrapper — CastPlayer.Builder handles everything |

## Files to Modify

| File | Changes |
|------|---------|
| `AndroidManifest.xml` | Replace meta-data value, add MediaTransferReceiver |
| `PlayerViewModel.kt` | Remove CastHelper/CastContext imports, use CastPlayer.Builder, add DeviceInfo listener |
| `MobilePlayerScreen.kt` | Remove CastButtonFactory reflection, use plain MediaRouteButton |
| `proguard-rules.pro` | Add MediaRouteActionProvider keep rule |

## Testing Checklist

1. **Build**: `GRADLE_USER_HOME=/tmp/.gradle ./gradlew assembleMobileRelease`
2. **Install**: No crash on app launch
3. **Play video**: No crash when clicking a movie
4. **Cast button**: Visible in player controls (if Cast device available)
5. **Cast playback**: Media plays on Cast device when connected
6. **Disconnect**: Returns to local playback seamlessly
7. **Release build**: R8 minification doesn't break Cast classes

## References

- [Media3 CastPlayer docs](https://developer.android.com/media/media3/cast/create-castplayer)
- [Media3 Cast demo](https://github.com/androidx/media/tree/release/demos/cast)
- [DefaultCastOptionsProvider source](https://github.com/androidx/media/blob/release/libraries/cast/src/main/java/androidx/media3/cast/DefaultCastOptionsProvider.java)
- [Google Cast codelab](https://developers.google.com/cast/codelabs/cast-videos-android)
