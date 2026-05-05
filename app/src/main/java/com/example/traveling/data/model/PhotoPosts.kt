package com.example.traveling.data.model

import com.google.firebase.Timestamp

/** photoPosts/{postId} */
data class PhotoPostDocument(
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String? = null,
    val title: String = "",
    val description: String = "",
    val imageUrls: List<String> = emptyList(),
    val locationName: String = "",
    val country: String? = null,
    val city: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationPrecision: String = "exact",
    val placeType: String = "",
    val tags: List<String> = emptyList(),
    val visibility: String = "public",
    val groupId: String? = null,
    val groupName: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val takenAt: Timestamp? = null,
    val periodLabel: String? = null,
    val likeCount: Int = 0,
    val saveCount: Int = 0,
    val commentCount: Int = 0,
    val reportCount: Int = 0,
    val isLinkedToTravelPath: Boolean = false,
    val travelPathPlaceId: String? = null,
    val status: String = "published"
)

/** photoPosts/{postId}/comments/{commentId} */
data class PhotoCommentDocument(
    val commentId: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String? = null,
    val content: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val status: String = "visible"
)

/** photoPosts/{postId}/likes/{userId} */
data class PostLikeDocument(
    val userId: String = "",
    val postId: String = "",
    val createdAt: Timestamp? = null
)

/** photoPosts/{postId}/saves/{userId} */
data class PostSaveDocument(
    val userId: String = "",
    val postId: String = "",
    val createdAt: Timestamp? = null,
    val collectionName: String? = null
)
