package com.example.traveling.data.model

object FirestoreCollections {
    const val USERS = "users"
    const val PHOTO_POSTS = "photoPosts"
    const val GROUPS = "groups"
    const val NOTIFICATIONS = "notifications"
    const val REPORTS = "reports"
    const val DESTINATIONS = "destinations"
    const val ATTRACTIONS = "attractions"

    const val COMMENTS = "comments"
    const val LIKES = "likes"
    const val SAVES = "saves"
    const val MEMBERS = "members"

    const val LIKED_POSTS = "likedPosts"
    const val SAVED_POSTS = "savedPosts"
    const val JOINED_GROUPS = "joinedGroups"
    const val NOTIFICATION_SETTINGS = "notificationSettings"
    const val DEFAULT_SETTINGS_DOC = "default"

    fun userDoc(userId: String): String = "$USERS/$userId"
    fun photoPostDoc(postId: String): String = "$PHOTO_POSTS/$postId"
    fun groupDoc(groupId: String): String = "$GROUPS/$groupId"
    fun notificationDoc(notificationId: String): String = "$NOTIFICATIONS/$notificationId"
    fun reportDoc(reportId: String): String = "$REPORTS/$reportId"

    fun postComments(postId: String): String = "$PHOTO_POSTS/$postId/$COMMENTS"
    fun postCommentDoc(postId: String, commentId: String): String = "$PHOTO_POSTS/$postId/$COMMENTS/$commentId"

    fun postLikes(postId: String): String = "$PHOTO_POSTS/$postId/$LIKES"
    fun postLikeDoc(postId: String, userId: String): String = "$PHOTO_POSTS/$postId/$LIKES/$userId"

    fun postSaves(postId: String): String = "$PHOTO_POSTS/$postId/$SAVES"
    fun postSaveDoc(postId: String, userId: String): String = "$PHOTO_POSTS/$postId/$SAVES/$userId"

    fun userLikedPosts(userId: String): String = "$USERS/$userId/$LIKED_POSTS"
    fun userLikedPostDoc(userId: String, postId: String): String = "$USERS/$userId/$LIKED_POSTS/$postId"

    fun userSavedPosts(userId: String): String = "$USERS/$userId/$SAVED_POSTS"
    fun userSavedPostDoc(userId: String, postId: String): String = "$USERS/$userId/$SAVED_POSTS/$postId"

    fun groupMembers(groupId: String): String = "$GROUPS/$groupId/$MEMBERS"
    fun groupMemberDoc(groupId: String, userId: String): String = "$GROUPS/$groupId/$MEMBERS/$userId"

    fun userJoinedGroups(userId: String): String = "$USERS/$userId/$JOINED_GROUPS"
    fun userJoinedGroupDoc(userId: String, groupId: String): String = "$USERS/$userId/$JOINED_GROUPS/$groupId"

    fun userNotificationSettings(userId: String): String = "$USERS/$userId/$NOTIFICATION_SETTINGS"
    fun userDefaultNotificationSettingsDoc(userId: String): String = "$USERS/$userId/$NOTIFICATION_SETTINGS/$DEFAULT_SETTINGS_DOC"
}
