package com.example.traveling.data.model

import com.google.firebase.Timestamp

/** groups/{groupId} */
data class GroupDocument(
    val groupId: String = "",
    val name: String = "",
    val description: String = "",
    val coverImageUrl: String? = null,
    val ownerId: String = "",
    val ownerName: String = "",
    val memberCount: Int = 0,
    val postCount: Int = 0,
    val visibility: String = "public",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

/** groups/{groupId}/members/{userId} */
data class GroupMemberDocument(
    val userId: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    val role: String = "member",
    val joinedAt: Timestamp? = null
)
