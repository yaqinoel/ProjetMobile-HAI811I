package com.example.traveling.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
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
import com.example.traveling.features.profile.LikedPostsScreen
import com.example.traveling.features.profile.MyPublishedPostsScreen
import com.example.traveling.features.profile.SavedPostsScreen
import com.example.traveling.features.travelshare.GroupsScreen
import com.example.traveling.features.travelshare.PhotoPostDetailScreen
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
                    // Static settings panel is handled inside NotificationsScreen.
                },
                onOpenPhotoDetail = { photoId ->
                    navController.navigate("photo_detail/$photoId")
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
                onPublishSuccess = {
                    navController.navigate("main") {
                        popUpTo("main") { inclusive = true }
                    }
                },
                onOpenMapPicker = {
                    // Static map picker is handled inside PublishPhotosScreen.
                }
            )
        }
        composable("my_published_posts") {
            MyPublishedPostsScreen(
                onBack = { navController.popBackStack() },
                onOpenPhotoDetail = { photoId -> navController.navigate("photo_detail/$photoId") }
            )
        }
        composable("liked_posts") {
            LikedPostsScreen(
                onBack = { navController.popBackStack() },
                onOpenPhotoDetail = { photoId -> navController.navigate("photo_detail/$photoId") }
            )
        }
        composable("saved_posts") {
            SavedPostsScreen(
                onBack = { navController.popBackStack() },
                onOpenPhotoDetail = { photoId -> navController.navigate("photo_detail/$photoId") }
            )
        }

        // ── 照片详情页 ──
        composable(
            route = "photo_detail/{photoId}",
            arguments = listOf(navArgument("photoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getString("photoId") ?: ""
            PhotoPostDetailScreen(
                photoId = photoId,
                isAnonymous = false,
                onBack = { navController.popBackStack() },
                onNavigateLogin = { navController.navigate("login") },
                onNavigateRegister = { navController.navigate("register") }
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
                onNavigateLogin = { navController.navigate("login") },
                onNavigateRegister = { navController.navigate("register") },
                onNavigateToNotifications = { navController.navigate("notifications") },
                onNavigateToGroups = { navController.navigate("groups") },
                onNavigateToMyPublishedPosts = { navController.navigate("my_published_posts") },
                onNavigateToLikedPosts = { navController.navigate("liked_posts") },
                onNavigateToSavedPosts = { navController.navigate("saved_posts") },
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
                onNavigateToMyPublishedPosts = { navController.navigate("login") },
                onNavigateToLikedPosts = { navController.navigate("login") },
                onNavigateToSavedPosts = { navController.navigate("login") },
                // 匿名状态下如果也允许看照片详情，也把回调接上
                onNavigateToPhotoDetail = { photoId ->
                    navController.navigate("photo_detail_anonymous/$photoId")
                }
            )
        }

        composable(
            route = "photo_detail_anonymous/{photoId}",
            arguments = listOf(navArgument("photoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getString("photoId") ?: ""
            PhotoPostDetailScreen(
                photoId = photoId,
                isAnonymous = true,
                onBack = { navController.popBackStack() },
                onNavigateLogin = { navController.navigate("login") },
                onNavigateRegister = { navController.navigate("register") }
            )
        }

    }
}
