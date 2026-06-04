package com.telegramtv.ui.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.telegramtv.data.repository.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.compose.runtime.*
import androidx.compose.runtime.compositionLocalOf
import androidx.activity.enableEdgeToEdge
import android.content.res.Configuration

val LocalPipMode = compositionLocalOf { false }

@AndroidEntryPoint
class MobileMainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var exoPlayer: androidx.media3.exoplayer.ExoPlayer

    companion object {
        const val ACTION_PIP_PLAY_PAUSE = "com.telegramtv.PIP_PLAY_PAUSE"
        const val ACTION_PIP_MUTE_UNMUTE = "com.telegramtv.PIP_MUTE_UNMUTE"
    }

    private var _isInPipMode by mutableStateOf(false)
    private var _isMuted by mutableStateOf(false)

    private val pipReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
            when (intent.action) {
                ACTION_PIP_PLAY_PAUSE -> {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                    } else {
                        exoPlayer.play()
                    }
                    updatePipParams()
                }
                ACTION_PIP_MUTE_UNMUTE -> {
                    _isMuted = !_isMuted
                    exoPlayer.volume = if (_isMuted) 0f else 1f
                    updatePipParams()
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        // Only enter PIP for video. Audio uses background playback service.
        if (exoPlayer.isPlaying || exoPlayer.playWhenReady) {
            // Check if current media has video tracks
            val hasVideo = exoPlayer.currentTracks.groups.any { group ->
                group.type == androidx.media3.common.C.TRACK_TYPE_VIDEO
            }
            if (hasVideo) {
                enterPip()
            }
            // For audio-only, onLeavePlayer in ViewModel handles background service
        }
    }

    private fun updatePipParams() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val actions = mutableListOf<android.app.RemoteAction>()

            // Play/Pause Action
            val playPauseIcon = if (exoPlayer.isPlaying) {
                android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_media_pause)
            } else {
                android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_media_play)
            }
            val playPauseIntent = android.app.PendingIntent.getBroadcast(
                this, 0, android.content.Intent(ACTION_PIP_PLAY_PAUSE).apply { 
                    `package` = packageName 
                }, 
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            actions.add(android.app.RemoteAction(
                playPauseIcon, 
                if (exoPlayer.isPlaying) "Pause" else "Play", 
                if (exoPlayer.isPlaying) "Pause" else "Play", 
                playPauseIntent
            ))

            // Mute/Unmute Action
            val muteIcon = if (_isMuted) {
                android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_lock_silent_mode)
            } else {
                android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_lock_silent_mode_off)
            }
            val muteIntent = android.app.PendingIntent.getBroadcast(
                this, 1, android.content.Intent(ACTION_PIP_MUTE_UNMUTE).apply { 
                    `package` = packageName 
                }, 
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            actions.add(android.app.RemoteAction(
                muteIcon, 
                if (_isMuted) "Unmute" else "Mute", 
                if (_isMuted) "Unmute" else "Mute", 
                muteIntent
            ))

            val builder = android.app.PictureInPictureParams.Builder()
                .setAspectRatio(android.util.Rational(16, 9))
                .setActions(actions)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                builder.setAutoEnterEnabled(true)
            }
            
            val params = builder.build()
            setPictureInPictureParams(params)
        }
    }

    fun enterPip() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            updatePipParams()
            // The params are already set by updatePipParams
            enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().build())
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        _isInPipMode = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            val filter = android.content.IntentFilter().apply {
                addAction(ACTION_PIP_PLAY_PAUSE)
                addAction(ACTION_PIP_MUTE_UNMUTE)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(pipReceiver, filter, RECEIVER_EXPORTED)
            } else {
                registerReceiver(pipReceiver, filter)
            }
        } else {
            try { unregisterReceiver(pipReceiver) } catch (e: Exception) {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Permission Logic
            val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_VIDEO,
                    android.Manifest.permission.READ_MEDIA_AUDIO
                )
            } else {
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }

            val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
            ) { /* Handle result if needed */ }

            androidx.compose.runtime.LaunchedEffect(Unit) {
                launcher.launch(permissions)
            }

            // Observe login state
            val isLoggedIn by authRepository.isLoggedIn.collectAsState(initial = false)
            val startDestination = if (isLoggedIn) "dashboard" else "login"

             CompositionLocalProvider(LocalPipMode provides _isInPipMode) {
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MobileApp(startDestination = startDestination)
                    }
                }
            }
        }
    }
}

