package com.qixev.qfieldkit.ui.navigation

sealed class Screen(val route: String) {
    data object Inventory : Screen("inventory")
}

