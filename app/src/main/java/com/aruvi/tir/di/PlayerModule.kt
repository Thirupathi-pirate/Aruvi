package com.aruvi.tir.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import com.aruvi.tir.data.repository.AuthRepository
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
    fun provideDataSourceFactory(
        @ApplicationContext context: Context,
        authRepository: AuthRepository
    ): DefaultDataSource.Factory {
        val httpFactory = DefaultHttpDataSource.Factory().apply {
            val headers = runBlocking {
                val token = authRepository.getAccessToken()
                if (token != null) mapOf("Authorization" to "Bearer $token") else emptyMap()
            }
            setDefaultRequestProperties(headers)
            setConnectTimeoutMs(30_000)
            setReadTimeoutMs(60_000)
            setAllowCrossProtocolRedirects(true)
        }
        
        return DefaultDataSource.Factory(context, httpFactory)
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
        // Increase buffer sizes for smoother streaming
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                32_000, // min buffer
                64_000, // max buffer
                2_500,  // buffer for playback
                5_000   // buffer for rebuffering
            )
            .build()

        return ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }
}
