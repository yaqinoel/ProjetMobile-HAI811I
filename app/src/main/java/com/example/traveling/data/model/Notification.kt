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

object NotificationTypes {
    const val USER_PUBLISH = "user_publish"
    const val GROUP_PUBLISH = "group_publish"
    const val TAG_MATCH = "tag_match"
    const val PLACE_TYPE_MATCH = "place_type_match"
    const val LIKE = "like"
    const val COMMENT = "comment"
    const val SYSTEM = "system"
}
