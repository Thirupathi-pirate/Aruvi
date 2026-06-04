package com.telegramtv.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.telegramtv.data.repository.AuthRepository
import com.telegramtv.ui.auth.LoginScreen
import com.telegramtv.ui.browse.FolderScreen
import com.telegramtv.ui.details.DetailsScreen
import com.telegramtv.ui.home.HomeScreen
import com.telegramtv.ui.player.PlayerScreen
import com.telegramtv.ui.search.SearchScreen
import com.telegramtv.ui.settings.SettingsScreen

/**
 * Main navigation graph for the app.
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    authRepository: AuthRepository
) {
    val isLoggedIn by authRepository.isLoggedIn.collectAsState(initial = false)
    
    val startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Login Screen
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Home Screen
        composable(Screen.Home.route) {
            HomeScreen(
                onFileClick = { fileId ->
                    navController.navigate(Screen.Details.createRoute(fileId))
                },
                onFolderClick = { folderId ->
                    navController.navigate(Screen.Folder.createRoute(folderId))
                },
                onSearchClick = {
                    navController.navigate(Screen.Search.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // Folder Screen
        composable(
            route = Screen.Folder.route,
            arguments = listOf(
                navArgument("folderId") { type = NavType.IntType }
            )
        ) {
            FolderScreen(
                onFileClick = { fileId ->
                    navController.navigate(Screen.Details.createRoute(fileId))
                },
                onFolderClick = { subFolderId ->
                    navController.navigate(Screen.Folder.createRoute(subFolderId))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // File Details Screen
        composable(
            route = Screen.Details.route,
            arguments = listOf(
                navArgument("fileId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val fileId = backStackEntry.arguments?.getInt("fileId") ?: return@composable
            DetailsScreen(
                fileId = fileId,
                onPlayClick = { id, resumePosition ->
                    navController.navigate(Screen.Player.createRoute(id, resumePosition))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Player Screen
        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("fileId") { type = NavType.IntType },
                navArgument("startPosition") { type = NavType.LongType; defaultValue = 0L }
            )
        ) {
            PlayerScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Search Screen
        composable(Screen.Search.route) {
            SearchScreen(
                onFileClick = { fileId ->
                    navController.navigate(Screen.Details.createRoute(fileId))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Settings Screen
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
