package com.example.traveling

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.traveling.features.passerelle.LaunchScreen
import com.example.traveling.features.passerelle.LoginScreen
import com.example.traveling.core.navigation.MainScreen
import com.example.traveling.ui.theme.TravelingTheme
import com.google.firebase.auth.FirebaseAuth
import com.example.traveling.data.repository.FirestoreSeeder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ── 首次启动时向 Firestore 写入预设数据（只执行一次）──
        /*
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("firestore_seeded", false)) {
            FirestoreSeeder.seedAll()
            prefs.edit().putBoolean("firestore_seeded", true).apply()
        }
        */
        setContent {
            TravelingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val startDest = if (auth.currentUser != null) "main" else "home"

    NavHost(navController = navController, startDestination = startDest) {
        composable("home") {
            LaunchScreen(
                onNavigateLogin = { navController.navigate("login") },
                onNavigateAnonymous = {
                    navController.navigate("main_anonymous") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
        composable("login") {
            LoginScreen(
                onBack = { navController.popBackStack() },
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNavigateRegister = { /* TODO: register screen */ }
            )
        }
        composable("main") {
            MainScreen(
                isAnonymous = false,
                onLogout = {
                    auth.signOut()
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("main_anonymous") {
            MainScreen(
                isAnonymous = true,
                onLogout = {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}