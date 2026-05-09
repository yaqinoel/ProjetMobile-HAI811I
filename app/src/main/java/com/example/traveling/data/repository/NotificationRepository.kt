package com.example.traveling.data.repository

import com.example.traveling.data.model.FirestoreCollections
import com.example.traveling.data.model.NotificationDocument
import com.example.traveling.data.model.NotificationSettingsDocument
import com.example.traveling.data.model.PhotoPostDocument
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class CreateNotificationInput(
    val receiverId: String,
    val type: String,
    val title: String,
    val message: String,
    val relatedPostId: String? = null,
    val relatedGroupId: String? = null,
    val relatedUserId: String? = null,
    val relatedPlaceId: String? = null
)

class NotificationRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun observeMyNotifications(
        userId: String,
        onChanged: (List<NotificationDocument>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection(FirestoreCollections.NOTIFICATIONS)
            .whereEqualTo("receiverId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents
                    ?.mapNotNull { it.toObject(NotificationDocument::class.java) }
                    ?.sortedWith(
                        compareByDescending<NotificationDocument> { it.createdAt?.seconds ?: 0L }
                            .thenByDescending { it.createdAt?.nanoseconds ?: 0 }
                    )
                    ?: emptyList()
                onChanged(list)
            }
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return runCatching {
            db.collection(FirestoreCollections.NOTIFICATIONS)
                .document(notificationId)
                .update("isRead", true)
                .awaitResult()
        }
    }

    suspend fun markAllAsRead(userId: String): Result<Unit> {
        return runCatching {
            val docs = db.collection(FirestoreCollections.NOTIFICATIONS)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .awaitResult()
                .documents

            if (docs.isEmpty()) return@runCatching

            val batch = db.batch()
            docs.forEach { batch.update(it.reference, "isRead", true) }
            batch.commit().awaitResult()
        }
    }

    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return runCatching {
            db.collection(FirestoreCollections.NOTIFICATIONS)
                .document(notificationId)
                .delete()
                .awaitResult()
        }
    }

    suspend fun getNotificationSettings(userId: String): NotificationSettingsDocument? {
        val doc = db.collection(FirestoreCollections.USERS)
            .document(userId)
            .collection(FirestoreCollections.NOTIFICATION_SETTINGS)
            .document(FirestoreCollections.DEFAULT_SETTINGS_DOC)
            .get()
            .awaitResult()
        return doc.toObject(NotificationSettingsDocument::class.java)
    }

    fun observeNotificationSettings(
        userId: String,
        onChanged: (NotificationSettingsDocument) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection(FirestoreCollections.USERS)
            .document(userId)
            .collection(FirestoreCollections.NOTIFICATION_SETTINGS)
            .document(FirestoreCollections.DEFAULT_SETTINGS_DOC)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                onChanged(snapshot?.toObject(NotificationSettingsDocument::class.java) ?: NotificationSettingsDocument())
            }
    }

    suspend fun updateNotificationSettings(
        userId: String,
        settings: NotificationSettingsDocument
    ): Result<Unit> {
        return runCatching {
            db.collection(FirestoreCollections.USERS)
                .document(userId)
                .collection(FirestoreCollections.NOTIFICATION_SETTINGS)
                .document(FirestoreCollections.DEFAULT_SETTINGS_DOC)
                .set(settings, SetOptions.merge())
                .awaitResult()
        }
    }

    suspend fun createNotification(input: CreateNotificationInput): Result<String> {
        return runCatching {
            val ref = db.collection(FirestoreCollections.NOTIFICATIONS).document()
            val doc = NotificationDocument(
                notificationId = ref.id,
                receiverId = input.receiverId,
                type = input.type,
                title = input.title,
                message = input.message,
                relatedPostId = input.relatedPostId,
                relatedGroupId = input.relatedGroupId,
                relatedUserId = input.relatedUserId,
                relatedPlaceId = input.relatedPlaceId,
                isRead = false,
                createdAt = Timestamp.now()
            )
            ref.set(doc).awaitResult()
            ref.id
        }
    }

    suspend fun createNotificationsForNewPost(post: PhotoPostDocument): Result<Unit> {
        return runCatching {
            val usersSnapshot = db.collection(FirestoreCollections.USERS).get().awaitResult()
            val batch = db.batch()

            if (post.visibility == "group" && !post.groupId.isNullOrBlank()) {
                val members = db.collection(FirestoreCollections.GROUPS)
                    .document(post.groupId)
                    .collection(FirestoreCollections.MEMBERS)
                    .get()
                    .awaitResult()
                    .documents

                members.forEach { member ->
                    val receiverId = member.id
                    if (receiverId == post.authorId) return@forEach

                    val settings = getNotificationSettings(receiverId) ?: NotificationSettingsDocument()
                    if (!settings.notifyFromGroups) return@forEach

                    val ref = db.collection(FirestoreCollections.NOTIFICATIONS).document()
                    batch.set(
                        ref,
                        NotificationDocument(
                            notificationId = ref.id,
                            receiverId = receiverId,
                            type = "group_publish",
                            title = post.groupName ?: "Nouveau post de groupe",
                            message = "${post.authorName} a publié une photo dans le groupe.",
                            relatedPostId = post.postId,
                            relatedGroupId = post.groupId,
                            relatedUserId = post.authorId,
                            relatedPlaceId = null,
                            isRead = false,
                            createdAt = Timestamp.now()
                        )
                    )
                }
            }

            if (post.visibility == "public") {
                usersSnapshot.documents.forEach { userDoc ->
                    val receiverId = userDoc.id
                    if (receiverId == post.authorId) return@forEach

                    val settings = getNotificationSettings(receiverId) ?: NotificationSettingsDocument()
                    val matchedTag = post.tags.firstOrNull { tag ->
                        settings.followedTags.any { it.equals(tag, ignoreCase = true) }
                    }
                    val matchPlaceType = settings.followedPlaceTypes.any {
                        it.equals(post.placeType, ignoreCase = true)
                    }

                    if (matchedTag != null && settings.notifyByTags) {
                        val ref = db.collection(FirestoreCollections.NOTIFICATIONS).document()
                        batch.set(
                            ref,
                            NotificationDocument(
                                notificationId = ref.id,
                                receiverId = receiverId,
                                type = "tag_match",
                                title = "Tag suivi : #$matchedTag",
                                message = "Nouvelle publication correspondante : ${post.title.ifBlank { post.locationName }}",
                                relatedPostId = post.postId,
                                relatedGroupId = null,
                                relatedUserId = post.authorId,
                                relatedPlaceId = null,
                                isRead = false,
                                createdAt = Timestamp.now()
                            )
                        )
                    }

                    if (matchPlaceType && settings.notifyByPlaces) {
                        val ref = db.collection(FirestoreCollections.NOTIFICATIONS).document()
                        batch.set(
                            ref,
                            NotificationDocument(
                                notificationId = ref.id,
                                receiverId = receiverId,
                                type = "place_type_match",
                                title = "Type de lieu suivi : ${post.placeType}",
                                message = "Nouvelle publication correspondante : ${post.title.ifBlank { post.locationName }}",
                                relatedPostId = post.postId,
                                relatedGroupId = null,
                                relatedUserId = post.authorId,
                                relatedPlaceId = post.city ?: post.country,
                                isRead = false,
                                createdAt = Timestamp.now()
                            )
                        )
                    }
                }
            }

            batch.commit().awaitResult()
        }
    }

    suspend fun createCommentNotificationIfNeeded(
        post: PhotoPostDocument,
        actorUserId: String,
        actorName: String
    ): Result<Unit> {
        if (post.authorId == actorUserId) return Result.success(Unit)
        val settings = getNotificationSettings(post.authorId) ?: NotificationSettingsDocument()
        if (!settings.notifyComments) return Result.success(Unit)

        return createNotification(
            CreateNotificationInput(
                receiverId = post.authorId,
                type = "comment",
                title = actorName.ifBlank { "Un voyageur" },
                message = "a commenté votre publication \"${post.title.ifBlank { post.locationName }}\"",
                relatedPostId = post.postId,
                relatedUserId = actorUserId
            )
        ).map { Unit }
    }

    suspend fun createLikeNotificationIfNeeded(
        post: PhotoPostDocument,
        actorUserId: String,
        actorName: String
    ): Result<Unit> {
        if (post.authorId == actorUserId) return Result.success(Unit)
        val settings = getNotificationSettings(post.authorId) ?: NotificationSettingsDocument()
        if (!settings.notifyLikes) return Result.success(Unit)

        return createNotification(
            CreateNotificationInput(
                receiverId = post.authorId,
                type = "like",
                title = actorName.ifBlank { "Un voyageur" },
                message = "a aimé votre publication \"${post.title.ifBlank { post.locationName }}\"",
                relatedPostId = post.postId,
                relatedUserId = actorUserId
            )
        ).map { Unit }
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { ex ->
        if (cont.isActive) cont.resumeWithException(ex)
    }
}
