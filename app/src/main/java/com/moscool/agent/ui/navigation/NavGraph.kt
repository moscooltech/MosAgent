package com.moscool.agent.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Agent : Screen("agent", "Agent", null)
    data object Preview : Screen("preview", "Preview", null)
    data object History : Screen("history", "History", Icons.Default.History)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object Onboarding : Screen("onboarding", "Onboarding", null)
}

val bottomNavItems = listOf(Screen.Home, Screen.History, Screen.Settings)
