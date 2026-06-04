package com.telegramtv.ui.navigation

/**
 * Navigation routes for the app.
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Folder : Screen("folder/{folderId}") {
        fun createRoute(folderId: Int) = "folder/$folderId"
    }
    object Details : Screen("details/{fileId}") {
        fun createRoute(fileId: Int) = "details/$fileId"
    }
    object Player : Screen("player/{fileId}?startPosition={startPosition}") {
        fun createRoute(fileId: Int, startPosition: Long = 0L) = "player/$fileId?startPosition=$startPosition"
    }
    object Search : Screen("search")
    object Settings : Screen("settings")
}
