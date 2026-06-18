package com.aruvi.tir.di

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider

class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        return try {
            CastOptions.Builder()
                .setReceiverApplicationId("CC1AD845")
                .build()
        } catch (_: Throwable) {
            CastOptions.Builder().build()
        }
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
}
