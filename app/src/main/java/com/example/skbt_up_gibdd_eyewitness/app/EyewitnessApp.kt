package com.example.skbt_up_gibdd_eyewitness.app

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.skbt_up_gibdd_eyewitness.feature.chat.ChatScreen
import com.example.skbt_up_gibdd_eyewitness.feature.welcome.WelcomeScreen
import com.example.skbt_up_gibdd_eyewitness.EyewitnessApplication
import com.example.skbt_up_gibdd_eyewitness.feature.location.LocationPickerScreen
import com.example.skbt_up_gibdd_eyewitness.feature.location.StaticLocation

private object Route {
    const val Welcome = "welcome"
    const val Chat = "chat"
    const val LocationPicker = "location_picker"
}

@Composable
fun EyewitnessApp() {
    val navController = rememberNavController()
    val container = (LocalContext.current.applicationContext as EyewitnessApplication).container
    var selectedStaticLocation by remember { mutableStateOf<StaticLocation?>(null) }

    NavHost(navController = navController, startDestination = Route.Welcome) {
        composable(Route.Welcome) {
            WelcomeScreen(onStartClick = {
                container.deviceRepository.register().map {
                    navController.navigate(Route.Chat) { launchSingleTop = true }
                }
            })
        }
        composable(Route.Chat) {
            ChatScreen(
                onBackClick = navController::navigateUp,
                messageRepository = container.messageRepository,
                selectedStaticLocation = selectedStaticLocation,
                onStaticLocationConsumed = { selectedStaticLocation = null },
                onOpenStaticLocationPicker = { navController.navigate(Route.LocationPicker) },
            )
        }
        composable(Route.LocationPicker) {
            LocationPickerScreen(
                onBackClick = navController::navigateUp,
                onLocationSelected = { location ->
                    selectedStaticLocation = location
                    navController.navigateUp()
                },
            )
        }
    }
}
