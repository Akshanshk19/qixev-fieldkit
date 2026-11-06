package com.qixev.qfieldkit.ui.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object RepairJobs : Screen("jobs")
    data object JobDetail : Screen("jobs/{jobId}") {
        fun createRoute(jobId: Long) = "jobs/$jobId"
    }
    data object Inventory : Screen("inventory")
    data object Settings : Screen("settings")
}
