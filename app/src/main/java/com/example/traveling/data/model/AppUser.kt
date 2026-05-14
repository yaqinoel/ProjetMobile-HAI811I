package com.example.traveling.data.model

import com.google.firebase.Timestamp

data class User(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val bio: String? = null,
    val homeCity: String? = null,
    val createdAt: Timestamp? = null,
    val lastLoginAt: Timestamp? = null,
    val postCount: Int = 0,
    val likedCount: Int = 0,
    val savedCount: Int = 0,
    val groupCount: Int = 0,
    val isAnonymousUpgraded: Boolean = false
)

data class UserLikedPostDocument(
    val postId: String = "",
    val createdAt: Timestamp? = null
)

data class UserSavedPostDocument(
    val postId: String = "",
    val createdAt: Timestamp? = null,
    val collectionName: String? = null,
    val note: String? = null
)

data class UserJoinedGroupDocument(
    val groupId: String = "",
    val name: String = "",
    val role: String = "member",
    val joinedAt: Timestamp? = null
)

data class NotificationSettingsDocument(
    val notifyFromFollowedUsers: Boolean = true,
    val notifyFromGroups: Boolean = true,
    val notifyByTags: Boolean = true,
    val notifyByPlaces: Boolean = true,
    val notifyComments: Boolean = true,
    val notifyLikes: Boolean = true,
    val notifySaves: Boolean = true,
    val followedTags: List<String> = emptyList(),
    val followedPlaceTypes: List<String> = emptyList(),
    val followedGroupIds: List<String> = emptyList(),
    val followedUserIds: List<String> = emptyList()
)
