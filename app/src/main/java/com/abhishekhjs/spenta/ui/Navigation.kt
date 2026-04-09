package com.abhishekhjs.spenta.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Splash : Screen("splash", "Splash", Icons.Default.Home)
    object Onboarding : Screen("onboarding", "Onboarding", Icons.Default.Home)
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Spendings : Screen("spendings", "Spendings", Icons.Default.History)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val items = listOf(
    Screen.Home,
    Screen.Spendings,
    Screen.Settings
)
