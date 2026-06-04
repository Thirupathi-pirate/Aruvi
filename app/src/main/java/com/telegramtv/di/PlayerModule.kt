package com.telegramtv.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.telegramtv.data.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

/**
 * Hilt module for media player dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    /**
     * Provide RenderersFactory for ExoPlayer.
     * 
     * EXTENSION_RENDERER_MODE_ON means:
     * - Extension decoders will be used if available
     * - Standard ExoPlayer supports HEVC, VP9, Opus, AAC, MP3, and most common formats
     */
    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideRenderersFactory(
        @ApplicationContext context: Context
    ): DefaultRenderersFactory {
        return DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
    }

    /**
     * Provide HTTP data source factory with auth header.
     */
    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideHttpDataSourceFactory(
        authRepository: AuthRepository
    ): DefaultHttpDataSource.Factory {
        return DefaultHttpDataSource.Factory().apply {
            setDefaultRequestProperties(
                runBlocking {
                    val token = authRepository.getAccessToken()
                    if (token != null) {
                        mapOf("Authorization" to "Bearer $token")
                    } else {
                        emptyMap()
                    }
                }
            )
            setConnectTimeoutMs(30_000)
            setReadTimeoutMs(60_000)
            setAllowCrossProtocolRedirects(true)
        }
    }

    /**
     * Provide ExoPlayer instance.
     */
    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        renderersFactory: DefaultRenderersFactory
    ): ExoPlayer {
        return ExoPlayer.Builder(context, renderersFactory)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }
}
