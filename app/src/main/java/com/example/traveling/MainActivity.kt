package com.example.traveling

import android.os.Bundle
import android.widget.Toast
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
import com.example.traveling.features.passerelle.ForgotPasswordScreen
import com.example.traveling.features.passerelle.RegisterScreen
import com.example.traveling.features.travelshare.GroupsScreen
import com.example.traveling.features.travelshare.PublishPhotosScreen
import com.example.traveling.features.travelshare.notifications.NotificationsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ── 重新写入更新后的数据（含法国城市） ──
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
        composable("notifications") {
            NotificationsScreen(
                onBack = {
                    navController.popBackStack()
                },
                onOpenSettings = {
                    // TODO: 通知设置页，在这里写跳转
                    // navController.navigate("notification_settings")
                }
            )
        }
        composable("groups") {
            GroupsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable("publish_photos") {
            PublishPhotosScreen(
                onBack = {
                    // Cancels publishing, goes back to where the user came from
                    navController.popBackStack()
                },
                onPublish = {
                    // TODO: save the data to Firestore
                    navController.navigate("main") {
                        popUpTo("main") { inclusive = true }
                    }
                },
                onOpenMapPicker = {
                    // TODO: open map picker
                    // Toast.makeText(context, "Ouverture de Google Maps...", Toast.LENGTH_SHORT).show()
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
                onNavigateRegister = {
                    navController.navigate("register")
                },
                onNavigateForgotPwd = {
                    navController.navigate("forgot_password")
                }
            )
        }
        composable("forgot_password") {
            ForgotPasswordScreen(
                onBack = { navController.popBackStack() },
                onNavigateLogin = {
                    navController.popBackStack()
                }
            )
        }
        composable("register") {
            RegisterScreen(
                onBack = { navController.popBackStack() },
                onNavigateLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate("main") {
                        popUpTo("home") { inclusive = true }
                    }
                }
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
                },
                onNavigateToNotifications = {
                    navController.navigate("notifications")
                },
                onNavigateToGroups = {
                    navController.navigate("groups")
                },
                onNavigateToPublish = {
                    navController.navigate("publish_photos")
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
                },
                onNavigateLogin = {
                    navController.navigate("login")
                },
                onNavigateRegister = {
                    navController.navigate("register")
                }
            )
        }
    }
}