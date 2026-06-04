package com.telegramtv.ui.mobile

import androidx.compose.runtime.Composable
import com.telegramtv.ui.theme.TelePlayMobileTheme

@Composable
fun MobileApp(startDestination: String = "login") {
    TelePlayMobileTheme {
        MobileScaffold(startDestination = startDestination)
    }
}


