package com.example.traveling.data.model

import com.google.firebase.Timestamp

/** notifications/{notificationId} */
data class NotificationDocument(
    val notificationId: String = "",
    val receiverId: String = "",
    val type: String = "",
    val title: String = "",
    val message: String = "",
    val relatedPostId: String? = null,
    val relatedGroupId: String? = null,
    val relatedUserId: String? = null,
    val relatedPlaceId: String? = null,
    val isRead: Boolean = false,
    val createdAt: Timestamp? = null
)
