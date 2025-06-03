package com.example.demofirestore3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.example.demofirestore3.navigation.AppNavGraph
import com.example.demofirestore3.screens.AuthSwitcher
import com.example.demofirestore3.ui.theme.Demofirestore3Theme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔒 Chặn dark mode toàn app
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // 🔧 Khởi tạo Firebase
        FirebaseApp.initializeApp(this)

        // 🚀 Giao diện chính
        setContent {
            Demofirestore3Theme {
                var isLoggedIn by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }

                if (isLoggedIn) {
                    val navController = rememberNavController()
                    AppNavGraph(
                        navController = navController,
                        onLogout = {
                            FirebaseAuth.getInstance().signOut()
                            isLoggedIn = false
                        }
                    )
                } else {
                    AuthSwitcher(onLogin = { isLoggedIn = true })
                }
            }
        }
    }
}
