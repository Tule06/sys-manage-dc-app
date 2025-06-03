package com.example.demofirestore3.screens

import androidx.compose.runtime.*

@Composable
fun AuthSwitcher(onLogin: () -> Unit) {
    var showRegister by remember { mutableStateOf(false) }

    if (showRegister) {
        RegisterScreen(
            onBackToLogin = { showRegister = false }
        )
    } else {
        LoginScreen(
            onLoginSuccess = onLogin,
            onGoToRegister = { showRegister = true }
        )
    }
}
