package com.example.demofirestore3.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.demofirestore3.screens.*

@Composable
fun AppNavGraph(
    navController: NavHostController,
    onLogout: () -> Unit
) {
    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            HomeScreen(
                onNavigate = { navController.navigate(it) },
                onLogout = { onLogout() }
            )
        }

        composable("find_uid") {
            FindUidScreen(navController)
        }

        composable("user_list") {
            UserListScreen(navController)
        }

        composable("Vaccine") {
            VaccineLookupScreen(navController)
        }

        // ✅ Route duy nhất: vào là tự động tra theo người đăng nhập
        composable("Violation") {
            ViolationLookupScreen(navController = navController)
        }

        composable("profile") {
            ProfileScreen(navController)
        }

        composable("notification_screen") {
            NotificationScreen(navController)
        }
    }
}
