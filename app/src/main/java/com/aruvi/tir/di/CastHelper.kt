package com.aruvi.tir.di

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener

object CastHelper {

    fun createCastContext(context: Context): CastContext? {
        return try {
            CastContext.getSharedInstance(context)
        } catch (_: Throwable) {
            null
        }
    }

    fun registerSessionListener(
        castContext: CastContext,
        callbacks: SessionCallbacks
    ): Any? {
        return try {
            val listener = object : SessionManagerListener<CastSession> {
                override fun onSessionStarted(session: CastSession, sessionId: String) = callbacks.onStarted()
                override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) = callbacks.onResumed()
                override fun onSessionEnded(session: CastSession, error: Int) = callbacks.onEnded()
                override fun onSessionSuspended(session: CastSession, reason: Int) = callbacks.onSuspended()
                override fun onSessionStarting(session: CastSession) {}
                override fun onSessionStartFailed(session: CastSession, error: Int) {}
                override fun onSessionEnding(session: CastSession) {}
                override fun onSessionResuming(session: CastSession, sessionId: String) {}
                override fun onSessionResumeFailed(session: CastSession, error: Int) {}
            }
            castContext.sessionManager.addSessionManagerListener(listener, CastSession::class.java)
            listener
        } catch (_: Throwable) {
            null
        }
    }

    fun unregisterSessionListener(castContext: CastContext, listener: Any?) {
        try {
            @Suppress("UNCHECKED_CAST")
            castContext.sessionManager.removeSessionManagerListener(
                listener as SessionManagerListener<CastSession>,
                CastSession::class.java
            )
        } catch (_: Throwable) {}
    }

    fun castToDevice(
        castPlayer: Any?,
        exoPlayer: ExoPlayer,
        url: String,
        fileId: String,
        title: String?,
        thumbnailUrl: String?
    ) {
        try {
            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(title ?: "Aruvi")
                .setArtworkUri(thumbnailUrl?.let { Uri.parse(it) })
                .build()
            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setMediaId(fileId)
                .setMediaMetadata(mediaMetadata)
                .build()

            val clazz = castPlayer!!.javaClass
            exoPlayer.pause()
            clazz.getMethod("setMediaItem", MediaItem::class.java).invoke(castPlayer, mediaItem)
            clazz.getMethod("prepare").invoke(castPlayer)
            clazz.getMethod("play").invoke(castPlayer)
        } catch (_: Throwable) {}
    }

    fun stopCasting(castPlayer: Any?) {
        try {
            val clazz = castPlayer!!.javaClass
            clazz.getMethod("stop").invoke(castPlayer)
            clazz.getMethod("clearMediaItems").invoke(castPlayer)
        } catch (_: Throwable) {}
    }
}

data class SessionCallbacks(
    val onStarted: () -> Unit,
    val onResumed: () -> Unit,
    val onEnded: () -> Unit,
    val onSuspended: () -> Unit
)
