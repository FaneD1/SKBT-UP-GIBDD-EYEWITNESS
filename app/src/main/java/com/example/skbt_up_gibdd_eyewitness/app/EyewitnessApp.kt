package com.example.skbt_up_gibdd_eyewitness.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.skbt_up_gibdd_eyewitness.feature.chat.ChatScreen
import com.example.skbt_up_gibdd_eyewitness.feature.welcome.WelcomeScreen

private object Route {
    const val Welcome = "welcome"
    const val Chat = "chat"
}

@Composable
fun EyewitnessApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Route.Welcome) {
        composable(Route.Welcome) {
            WelcomeScreen(onStartClick = {
                navController.navigate(Route.Chat) { launchSingleTop = true }
            })
        }
        composable(Route.Chat) {
            ChatScreen(onBackClick = navController::navigateUp)
        }
    }
}
