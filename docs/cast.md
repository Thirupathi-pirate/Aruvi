# Chromecast Integration — Official Media3 CastPlayer

## Root Cause of Crashes

### Crash 1: PendingIntent FLAG_MUTABLE (FIXED in v0.2.1)

`play-services-cast-framework:21.4.0` uses `PendingIntent` with `FLAG_MUTABLE`, which Android 14+ (SDK 34+) **forbids**. This causes a native SIGSEGV crash that Java try-catch cannot catch.

**Fix**: Bump to `play-services-cast-framework:21.5.0`.

Reference: [androidx/media#2178](https://github.com/androidx/media/issues/2178)

### Crash 2: CastContext on main thread (FIXED in v0.2.0)

`CastContext.getSharedInstance()` blocks the main thread and can trigger native crashes on some devices.

**Fix**: Initialize CastContext on `Dispatchers.IO` (background thread) via `viewModelScope.launch`.

### Old issues (FIXED in v0.2.0)

1. Two conflicting APIs mixed: old Google Cast SDK + new Media3 CastPlayer
2. Reflection-based `CastPlayer` instantiation masked errors
3. Custom `CastOptionsProvider` with no error handling

## Current Integration (v0.2.1)

### Architecture

```
┌──────────────────────────────────────────────────┐
│  Manifest                                        │
│  └── DefaultCastOptionsProvider (Media3 built-in) │
├──────────────────────────────────────────────────┤
│  PlayerViewModel                                  │
│  ├── ExoPlayer (Hilt singleton)                   │
│  ├── CastContext on Dispatchers.IO (background)   │
│  ├── CastPlayer(castContext) with try-catch       │
│  ├── Player.Listener (DeviceInfo tracking)        │
│  └── direct setMediaItem/prepare/play on cast    │
├──────────────────────────────────────────────────┤
│  MobilePlayerScreen                               │
│  └── MediaRouteButton + CastButtonFactory (safe)  │
└──────────────────────────────────────────────────┘
```

### Dependencies

```kotlin
implementation("androidx.media3:media3-cast:1.2.1")
implementation("com.google.android.gms:play-services-cast-framework:21.5.0")  // NOT 21.4.0!
implementation("androidx.mediarouter:mediarouter:1.6.0")
```

### PlayerViewModel.kt — CastPlayer Creation (background thread)

```kotlin
private var castPlayer: CastPlayer? = null

private fun initCastPlayer() {
    viewModelScope.launch {
        val player = withContext(Dispatchers.IO) {
            try {
                val castContext = CastContext.getSharedInstance(context)
                CastPlayer(castContext)
            } catch (_: Throwable) {
                null
            }
        }
        castPlayer = player
        player?.addListener(castPlayerListener)
    }
}
```

### MobilePlayerScreen.kt — MediaRouteButton

```kotlin
AndroidView(
    factory = { ctx ->
        val btn = MediaRouteButton(ctx)
        try {
            CastButtonFactory.setUpMediaRouteButton(ctx, btn)
        } catch (_: Throwable) {}
        btn.layoutParams = ViewGroup.LayoutParams(buttonSizePx, buttonSizePx)
        btn
    },
    modifier = Modifier.size(48.dp)
)
```

### AndroidManifest.xml

```xml
<meta-data
    android:name="com.google.android.gms.cast.framework.OPTIONS_PROVIDER_CLASS_NAME"
    android:value="androidx.media3.cast.DefaultCastOptionsProvider" />
```

### ProGuard Rules

```proguard
-keep class com.google.android.gms.cast.** { *; }
-keep class com.google.android.gms.cast.framework.** { *; }
-keep class androidx.mediarouter.app.MediaRouteActionProvider { *; }
```

## Files Deleted (old broken code)

| File | Reason |
|------|--------|
| `di/CastModule.kt` | Hilt CastContext singleton — lazy init in ViewModel is safer |
| `di/CastOptionsProvider.kt` | Custom provider — replaced by DefaultCastOptionsProvider |
| `di/CastHelper.kt` | Reflection wrapper — direct API is cleaner |

## References

- [Media3 CastPlayer docs](https://developer.android.com/media/media3/cast/create-castplayer)
- [Media3 Cast demo](https://github.com/androidx/media/tree/release/demos/cast)
- [PendingIntent crash fix](https://github.com/androidx/media/issues/2178)
- [Google Cast release notes](https://developers.google.com/cast/docs/release-notes)
