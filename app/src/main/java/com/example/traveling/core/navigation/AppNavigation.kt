package com.example.traveling.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.traveling.features.main.MainScreen
import com.google.firebase.auth.FirebaseAuth

import com.example.traveling.features.auth.LaunchScreen
import com.example.traveling.features.auth.LoginScreen
import com.example.traveling.features.auth.ForgotPasswordScreen
import com.example.traveling.features.auth.RegisterScreen
import com.example.traveling.features.profile.LikedPostsScreen
import com.example.traveling.features.profile.LikedRoutesScreen
import com.example.traveling.features.profile.ImageMigrationScreen
import com.example.traveling.features.profile.FollowingManagementScreen
import com.example.traveling.features.profile.MyPublishedPostsScreen
import com.example.traveling.features.profile.SavedPostsScreen
import com.example.traveling.features.profile.SavedRoutesScreen
import com.example.traveling.features.travelshare.AuthorProfileScreen
import com.example.traveling.features.travelshare.PhotoPostDetailScreen
import com.example.traveling.features.travelshare.GroupDetailScreen
import com.example.traveling.features.travelshare.GroupsScreen
import com.example.traveling.features.travelshare.PublishPhotosScreen
import com.example.traveling.features.travelshare.NotificationsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val startDest = when {
        currentUser == null -> "home"
        currentUser.isAnonymous -> "main_anonymous"
        else -> "main"
    }

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
                },
                onOpenGroupDetail = { groupId ->
                    navController.navigate("group_detail/$groupId")
                }
            )
        }
        composable("groups") {
            GroupsScreen(
                onBack = {
                    navController.popBackStack()
                },
                onOpenGroupDetail = { groupId ->
                    navController.navigate("group_detail/$groupId")
                }
            )
        }
        composable(
            route = "group_detail/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            GroupDetailScreen(
                groupId = groupId,
                onBack = { navController.popBackStack() },
                onOpenPhotoDetail = { photoId -> navController.navigate("photo_detail/$photoId") }
            )
        }
        composable("publish_photos") {
            PublishPhotosScreen(
                onBack = {
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
        composable(
            route = "edit_publish/{postId}",
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            PublishPhotosScreen(
                editPostId = postId,
                onBack = {
                    navController.popBackStack()
                },
                onPublishSuccess = {
                    navController.popBackStack()
                },
                onOpenMapPicker = {
                    // Static map picker is handled inside PublishPhotosScreen.
                }
            )
        }
        composable("my_published_posts") {
            MyPublishedPostsScreen(
                onBack = { navController.popBackStack() },
                onOpenPhotoDetail = { photoId -> navController.navigate("photo_detail/$photoId") },
                onEditPost = { postId -> navController.navigate("edit_publish/$postId") }
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
        composable("following") {
            FollowingManagementScreen(
                onBack = { navController.popBackStack() },
                onOpenAuthorProfile = { userId -> navController.navigate("author_profile/$userId") }
            )
        }
        composable("liked_routes") {
            LikedRoutesScreen(
                onBack = { navController.popBackStack() },
                onOpenRoute = { routeId -> navController.navigate("route_detail_cached/$routeId") }
            )
        }
        composable("saved_routes") {
            SavedRoutesScreen(
                onBack = { navController.popBackStack() },
                onOpenRoute = { routeId -> navController.navigate("route_detail_cached/$routeId") }
            )
        }
        composable(
            route = "route_detail_cached/{routeId}",
            arguments = listOf(navArgument("routeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments?.getString("routeId") ?: ""
            CachedRouteDetailScreen(
                routeId = routeId,
                onBack = { navController.popBackStack() },
                onOpenPhotoDetail = { photoId -> navController.navigate("photo_detail/$photoId") }
            )
        }
        composable("image_migration") {
            ImageMigrationScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = "author_profile/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            AuthorProfileScreen(
                authorId = userId,
                onBack = { navController.popBackStack() },
                onOpenPhotoDetail = { photoId ->
                    val detailRoute = if (auth.currentUser?.isAnonymous == true) {
                        "photo_detail_anonymous/$photoId"
                    } else {
                        "photo_detail/$photoId"
                    }
                    navController.navigate(detailRoute)
                }
            )
        }

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
                onNavigateRegister = { navController.navigate("register") },
                onAuthorClick = { userId -> navController.navigate("author_profile/$userId") },
                onFindSimilarPhotos = { postId -> navController.navigate("main_similar/$postId") },
                onAddToTravelPath = { seed ->
                    navController.navigate("main_from_photo/${seed.sourcePostId}") {
                        popUpTo("main") { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = "main_from_photo/{photoId}",
            arguments = listOf(navArgument("photoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getString("photoId") ?: ""
            MainScreen(
                isAnonymous = false,
                initialTravelPathPostId = photoId,
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
                onNavigateToLikedRoutes = { navController.navigate("liked_routes") },
                onNavigateToSavedRoutes = { navController.navigate("saved_routes") },
                onNavigateToImageMigration = { navController.navigate("image_migration") },
                onNavigateToPublish = { navController.navigate("publish_photos") },
                onNavigateToPhotoDetail = { detailPhotoId ->
                    navController.navigate("photo_detail/$detailPhotoId")
                },
                onNavigateToAuthorProfile = { userId -> navController.navigate("author_profile/$userId") },
                onNavigateToGroupDetail = { groupId -> navController.navigate("group_detail/$groupId") },
                onNavigateToFollowing = { navController.navigate("following") }
            )
        }

        composable(
            route = "main_similar/{photoId}",
            arguments = listOf(navArgument("photoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getString("photoId") ?: ""
            MainScreen(
                isAnonymous = false,
                initialSimilarPostId = photoId,
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
                onNavigateToLikedRoutes = { navController.navigate("liked_routes") },
                onNavigateToSavedRoutes = { navController.navigate("saved_routes") },
                onNavigateToImageMigration = { navController.navigate("image_migration") },
                onNavigateToPublish = { navController.navigate("publish_photos") },
                onNavigateToPhotoDetail = { detailPhotoId ->
                    navController.navigate("photo_detail/$detailPhotoId")
                },
                onNavigateToAuthorProfile = { userId -> navController.navigate("author_profile/$userId") },
                onNavigateToGroupDetail = { groupId -> navController.navigate("group_detail/$groupId") },
                onNavigateToFollowing = { navController.navigate("following") }
            )
        }

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
                onNavigateToLikedRoutes = { navController.navigate("liked_routes") },
                onNavigateToSavedRoutes = { navController.navigate("saved_routes") },
                onNavigateToImageMigration = { navController.navigate("image_migration") },
                onNavigateToPublish = { navController.navigate("publish_photos") },
                onNavigateToPhotoDetail = { photoId ->
                    navController.navigate("photo_detail/$photoId")
                },
                onNavigateToAuthorProfile = { userId -> navController.navigate("author_profile/$userId") },
                onNavigateToGroupDetail = { groupId -> navController.navigate("group_detail/$groupId") },
                onNavigateToFollowing = { navController.navigate("following") }
            )
        }

        composable("main_anonymous") {
            MainScreen(
                isAnonymous = true,
                onLogout = { navController.navigate("home") { popUpTo(0) { inclusive = true } } },
                onNavigateLogin = {
                    auth.signOut()
                    navController.navigate("login")
                },
                onNavigateRegister = {
                    auth.signOut()
                    navController.navigate("register")
                },
                onNavigateToMyPublishedPosts = { navController.navigate("login") },
                onNavigateToLikedPosts = { navController.navigate("liked_posts") },
                onNavigateToSavedPosts = { navController.navigate("saved_posts") },
                onNavigateToPhotoDetail = { photoId ->
                    navController.navigate("photo_detail_anonymous/$photoId")
                },
                onNavigateToAuthorProfile = { userId -> navController.navigate("author_profile/$userId") },
                onNavigateToGroupDetail = { groupId -> navController.navigate("group_detail/$groupId") },
                onNavigateToFollowing = { navController.navigate("login") }
            )
        }

        composable(
            route = "main_anonymous_similar/{photoId}",
            arguments = listOf(navArgument("photoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getString("photoId") ?: ""
            MainScreen(
                isAnonymous = true,
                initialSimilarPostId = photoId,
                onLogout = { navController.navigate("home") { popUpTo(0) { inclusive = true } } },
                onNavigateLogin = {
                    auth.signOut()
                    navController.navigate("login")
                },
                onNavigateRegister = {
                    auth.signOut()
                    navController.navigate("register")
                },
                onNavigateToMyPublishedPosts = { navController.navigate("login") },
                onNavigateToLikedPosts = { navController.navigate("liked_posts") },
                onNavigateToSavedPosts = { navController.navigate("saved_posts") },
                onNavigateToPhotoDetail = { detailPhotoId ->
                    navController.navigate("photo_detail_anonymous/$detailPhotoId")
                },
                onNavigateToAuthorProfile = { userId -> navController.navigate("author_profile/$userId") },
                onNavigateToGroupDetail = { groupId -> navController.navigate("group_detail/$groupId") },
                onNavigateToFollowing = { navController.navigate("login") }
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
                onNavigateLogin = {
                    auth.signOut()
                    navController.navigate("login")
                },
                onNavigateRegister = {
                    auth.signOut()
                    navController.navigate("register")
                },
                onAuthorClick = { userId -> navController.navigate("author_profile/$userId") },
                onFindSimilarPhotos = { postId -> navController.navigate("main_anonymous_similar/$postId") },
                onAddToTravelPath = { seed ->
                    navController.navigate("main_anonymous_from_photo/${seed.sourcePostId}") {
                        popUpTo("main_anonymous") { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = "main_anonymous_from_photo/{photoId}",
            arguments = listOf(navArgument("photoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getString("photoId") ?: ""
            MainScreen(
                isAnonymous = true,
                initialTravelPathPostId = photoId,
                onLogout = { navController.navigate("home") { popUpTo(0) { inclusive = true } } },
                onNavigateLogin = {
                    auth.signOut()
                    navController.navigate("login")
                },
                onNavigateRegister = {
                    auth.signOut()
                    navController.navigate("register")
                },
                onNavigateToMyPublishedPosts = { navController.navigate("login") },
                onNavigateToLikedPosts = { navController.navigate("liked_posts") },
                onNavigateToSavedPosts = { navController.navigate("saved_posts") },
                onNavigateToPhotoDetail = { detailPhotoId ->
                    navController.navigate("photo_detail_anonymous/$detailPhotoId")
                },
                onNavigateToAuthorProfile = { userId -> navController.navigate("author_profile/$userId") },
                onNavigateToGroupDetail = { groupId -> navController.navigate("group_detail/$groupId") },
                onNavigateToFollowing = { navController.navigate("login") }
            )
        }

    }
}
