package com.aruvi.tir.di

import android.content.Context
import com.google.android.gms.cast.framework.CastContext
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CastModule {

    @Provides
    @Singleton
    fun provideCastContext(
        @ApplicationContext context: Context
    ): CastContext? {
        return try {
            CastContext.getSharedInstance(context)
        } catch (_: Exception) {
            null
        }
    }
}
