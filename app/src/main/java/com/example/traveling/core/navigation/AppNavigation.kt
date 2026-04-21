package com.example.traveling.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.traveling.features.main.MainScreen
import com.google.firebase.auth.FirebaseAuth

import com.example.traveling.features.passerelle.LaunchScreen
import com.example.traveling.features.passerelle.LoginScreen
import com.example.traveling.features.passerelle.ForgotPasswordScreen
import com.example.traveling.features.passerelle.RegisterScreen
import com.example.traveling.features.travelshare.GroupsScreen
import com.example.traveling.features.travelshare.PhotoDetailScreen
import com.example.traveling.features.travelshare.PublishPhotosScreen
import com.example.traveling.features.travelshare.notifications.NotificationsScreen

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
                    navController.navigate("main_anonymous") { popUpTo("home") { inclusive = true } }
                }
            )
        }
        composable("login") {
            LoginScreen(
                onBack = { navController.popBackStack() },
                onLoginSuccess = { navController.navigate("main") { popUpTo("home") { inclusive = true } } },
                onNavigateRegister = { navController.navigate("register") },
                onNavigateForgotPwd = { navController.navigate("forgot_password") }
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
                onNavigateLogin = { navController.popBackStack() },
                onRegisterSuccess = { navController.navigate("main") { popUpTo("home") { inclusive = true } } }
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

        // ── 照片详情页 ──
        composable(
            route = "photo_detail/{photoId}",
            arguments = listOf(navArgument("photoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getString("photoId") ?: ""
            PhotoDetailScreen(
                photoId = photoId,
                onBack = { navController.popBackStack() }
            )
        }

        // ── 主屏幕 (已登录) ──
        composable("main") {
            MainScreen(
                isAnonymous = false,
                onLogout = {
                    auth.signOut()
                    navController.navigate("home") { popUpTo(0) { inclusive = true } }
                },
                onNavigateToNotifications = { navController.navigate("notifications") },
                onNavigateToGroups = { navController.navigate("groups") },
                onNavigateToPublish = { navController.navigate("publish_photos") },
                onNavigateToPhotoDetail = { photoId ->
                    navController.navigate("photo_detail/$photoId")
                }
            )
        }

        // ── 主屏幕 (匿名) ──
        composable("main_anonymous") {
            MainScreen(
                isAnonymous = true,
                onLogout = { navController.navigate("home") { popUpTo(0) { inclusive = true } } },
                onNavigateLogin = { navController.navigate("login") },
                onNavigateRegister = { navController.navigate("register") },
                // 匿名状态下如果也允许看照片详情，也把回调接上
                onNavigateToPhotoDetail = { photoId ->
                    navController.navigate("photo_detail/$photoId")
                }
            )
        }

    }
}