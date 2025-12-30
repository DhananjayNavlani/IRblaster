package com.example.irblaster.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CustomRemote : Screen("custom_remote/{remoteId}") {
        fun createRoute(remoteId: Long) = "custom_remote/$remoteId"
    }
    object CreateRemote : Screen("create_remote")
    object EditRemote : Screen("edit_remote/{remoteId}") {
        fun createRoute(remoteId: Long) = "edit_remote/$remoteId"
    }
}

