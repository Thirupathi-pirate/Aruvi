package com.telegramtv.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.telegramtv.data.repository.AuthRepository
import com.telegramtv.ui.navigation.NavGraph
import com.telegramtv.ui.theme.TelePlayTheme
import com.telegramtv.ui.theme.TVBackground
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main Activity for TelePlay.
 * Serves as the entry point and hosts the Compose navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TelePlayTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = TVBackground
                ) {
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                        authRepository = authRepository
                    )
                }
            }
        }
    }
}
