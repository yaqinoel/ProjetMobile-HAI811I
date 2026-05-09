package com.example.traveling.data.repository

import android.net.Uri
import com.example.traveling.data.model.FirestoreCollections
import com.example.traveling.data.model.PhotoCommentDocument
import com.example.traveling.data.model.PhotoPostDocument
import com.example.traveling.data.model.PostLikeDocument
import com.example.traveling.data.model.PostSaveDocument
import com.example.traveling.data.model.UserLikedPostDocument
import com.example.traveling.data.model.UserSavedPostDocument
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class PublishPhotoPostInput(
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String?,
    val localImageUris: List<Uri>,
    val title: String,
    val description: String,
    val locationName: String,
    val locationAddress: String?,
    val googlePlaceId: String?,
    val locationSource: String?,
    val city: String?,
    val country: String?,
    val rawLatitude: Double?,
    val rawLongitude: Double?,
    val displayLatitude: Double?,
    val displayLongitude: Double?,
    val locationPrecision: String,
    val placeType: String,
    val tags: List<String>,
    val visibility: String,
    val groupId: String?,
    val groupName: String?,
    val isLinkedToTravelPath: Boolean
)

class PhotoPostRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val notificationRepository: NotificationRepository = NotificationRepository()
) {

    suspend fun publishPhotoPost(input: PublishPhotoPostInput): Result<String> {
        return runCatching {
            require(input.localImageUris.isNotEmpty()) { "Aucune image à publier" }

            val postRef = db.collection(FirestoreCollections.PHOTO_POSTS).document()
            val postId = postRef.id

            val uploadedUrls = uploadAllImages(
                authorId = input.authorId,
                postId = postId,
                uris = input.localImageUris
            )

            val now = Timestamp.now()
            val post = PhotoPostDocument(
                postId = postId,
                authorId = input.authorId,
                authorName = input.authorName,
                authorAvatarUrl = input.authorAvatarUrl,
                title = input.title,
                description = input.description,
                imageUrls = uploadedUrls,
                locationName = input.locationName,
                locationAddress = input.locationAddress,
                googlePlaceId = input.googlePlaceId,
                locationSource = input.locationSource,
                country = input.country,
                city = input.city,
                rawLatitude = input.rawLatitude,
                rawLongitude = input.rawLongitude,
                displayLatitude = input.displayLatitude,
                displayLongitude = input.displayLongitude,
                latitude = input.displayLatitude,
                longitude = input.displayLongitude,
                locationPrecision = input.locationPrecision,
                placeType = input.placeType,
                tags = input.tags,
                visibility = input.visibility,
                groupId = input.groupId,
                groupName = input.groupName,
                createdAt = now,
                updatedAt = now,
                takenAt = null,
                periodLabel = null,
                likeCount = 0,
                saveCount = 0,
                commentCount = 0,
                reportCount = 0,
                isLinkedToTravelPath = input.isLinkedToTravelPath,
                travelPathPlaceId = null,
                status = "published"
            )

            val userRef = db.collection(FirestoreCollections.USERS).document(input.authorId)
            val batch = db.batch()
            batch.set(postRef, post)
            batch.update(userRef, "postCount", FieldValue.increment(1))
            if (input.visibility == "group" && !input.groupId.isNullOrBlank()) {
                val groupRef = db.collection(FirestoreCollections.GROUPS).document(input.groupId)
                batch.set(groupRef, mapOf("postCount" to FieldValue.increment(1)), SetOptions.merge())
            }
            batch.commit().awaitResult()
            runCatching { notificationRepository.createNotificationsForNewPost(post) }

            postId
        }
    }

    fun observePublicPublishedPosts(
        onChanged: (List<PhotoPostDocument>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection(FirestoreCollections.PHOTO_POSTS)
            .whereEqualTo("status", "published")
            .whereEqualTo("visibility", "public")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents
                    ?.mapNotNull { it.toObject(PhotoPostDocument::class.java) }
                    ?.sortedByNewest()
                    ?: emptyList()
                onChanged(posts)
            }
    }

    fun observeMyPublishedPosts(
        userId: String,
        onChanged: (List<PhotoPostDocument>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection(FirestoreCollections.PHOTO_POSTS)
            .whereEqualTo("authorId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents
                    ?.mapNotNull { it.toObject(PhotoPostDocument::class.java) }
                    ?.filterNot { it.status == "deleted" }
                    ?.sortedByNewest()
                    ?: emptyList()
                onChanged(posts)
            }
    }

    fun observeGroupPublishedPosts(
        groupId: String,
        onChanged: (List<PhotoPostDocument>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection(FirestoreCollections.PHOTO_POSTS)
            .whereEqualTo("status", "published")
            .whereEqualTo("visibility", "group")
            .whereEqualTo("groupId", groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents
                    ?.mapNotNull { it.toObject(PhotoPostDocument::class.java) }
                    ?.sortedByNewest()
                    ?: emptyList()
                onChanged(posts)
            }
    }

    fun observePostDetail(
        postId: String,
        onChanged: (PhotoPostDocument?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection(FirestoreCollections.PHOTO_POSTS)
            .document(postId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                onChanged(snapshot?.toObject(PhotoPostDocument::class.java))
            }
    }

    fun observePostComments(
        postId: String,
        onChanged: (List<PhotoCommentDocument>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection(FirestoreCollections.PHOTO_POSTS)
            .document(postId)
            .collection(FirestoreCollections.COMMENTS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val comments = snapshot?.documents
                    ?.mapNotNull { it.toObject(PhotoCommentDocument::class.java) }
                    ?.sortedWith(
                        compareBy<PhotoCommentDocument> { it.createdAt?.seconds ?: 0L }
                            .thenBy { it.createdAt?.nanoseconds ?: 0 }
                    )
                    ?: emptyList()
                onChanged(comments)
            }
    }

    suspend fun addComment(
        postId: String,
        comment: PhotoCommentDocument
    ): Result<String> {
        return runCatching {
            val commentId = if (comment.commentId.isBlank()) {
                db.collection(FirestoreCollections.PHOTO_POSTS)
                    .document(postId)
                    .collection(FirestoreCollections.COMMENTS)
                    .document()
                    .id
            } else {
                comment.commentId
            }
            val commentRef = db.collection(FirestoreCollections.PHOTO_POSTS)
                .document(postId)
                .collection(FirestoreCollections.COMMENTS)
                .document(commentId)
            val postRef = db.collection(FirestoreCollections.PHOTO_POSTS).document(postId)

            val batch = db.batch()
            batch.set(
                commentRef,
                comment.copy(
                    commentId = commentId,
                    postId = postId,
                    createdAt = comment.createdAt ?: Timestamp.now(),
                    status = if (comment.status.isBlank()) "visible" else comment.status
                )
            )
            batch.update(postRef, "commentCount", FieldValue.increment(1))
            batch.update(postRef, "updatedAt", FieldValue.serverTimestamp())
            batch.commit().awaitResult()

            runCatching {
                val postDoc = postRef.get().awaitResult().toObject(PhotoPostDocument::class.java)
                if (postDoc != null) {
                    notificationRepository.createCommentNotificationIfNeeded(
                        post = postDoc,
                        actorUserId = comment.authorId,
                        actorName = comment.authorName
                    )
                }
            }
            commentId
        }
    }

    suspend fun likePost(userId: String, postId: String): Result<Unit> {
        return runCatching {
            db.runTransaction { transaction ->
                val postLikeRef = db.collection(FirestoreCollections.PHOTO_POSTS)
                    .document(postId)
                    .collection(FirestoreCollections.LIKES)
                    .document(userId)
                val userLikedRef = db.collection(FirestoreCollections.USERS)
                    .document(userId)
                    .collection(FirestoreCollections.LIKED_POSTS)
                    .document(postId)
                val postRef = db.collection(FirestoreCollections.PHOTO_POSTS).document(postId)
                val userRef = db.collection(FirestoreCollections.USERS).document(userId)

                val alreadyLiked = transaction.get(postLikeRef).exists()
                if (alreadyLiked) return@runTransaction Unit

                transaction.set(postLikeRef, PostLikeDocument(userId = userId, postId = postId, createdAt = Timestamp.now()))
                transaction.set(userLikedRef, UserLikedPostDocument(postId = postId, createdAt = Timestamp.now()))
                transaction.update(postRef, "likeCount", FieldValue.increment(1))
                transaction.set(userRef, mapOf("likedCount" to FieldValue.increment(1)), SetOptions.merge())
                Unit
            }.awaitResult()

            runCatching {
                val post = db.collection(FirestoreCollections.PHOTO_POSTS)
                    .document(postId)
                    .get()
                    .awaitResult()
                    .toObject(PhotoPostDocument::class.java)
                val actorName = db.collection(FirestoreCollections.USERS)
                    .document(userId)
                    .get()
                    .awaitResult()
                    .getString("displayName")
                    .orEmpty()
                if (post != null) {
                    notificationRepository.createLikeNotificationIfNeeded(
                        post = post,
                        actorUserId = userId,
                        actorName = actorName
                    )
                }
            }
        }
    }

    suspend fun unlikePost(userId: String, postId: String): Result<Unit> {
        return runCatching {
            db.runTransaction { transaction ->
                val postLikeRef = db.collection(FirestoreCollections.PHOTO_POSTS)
                    .document(postId)
                    .collection(FirestoreCollections.LIKES)
                    .document(userId)
                val userLikedRef = db.collection(FirestoreCollections.USERS)
                    .document(userId)
                    .collection(FirestoreCollections.LIKED_POSTS)
                    .document(postId)
                val postRef = db.collection(FirestoreCollections.PHOTO_POSTS).document(postId)
                val userRef = db.collection(FirestoreCollections.USERS).document(userId)

                val alreadyLiked = transaction.get(postLikeRef).exists()
                if (!alreadyLiked) return@runTransaction Unit

                transaction.delete(postLikeRef)
                transaction.delete(userLikedRef)
                transaction.update(postRef, "likeCount", FieldValue.increment(-1))
                transaction.set(userRef, mapOf("likedCount" to FieldValue.increment(-1)), SetOptions.merge())
                Unit
            }.awaitResult()
        }
    }

    suspend fun savePost(userId: String, postId: String, collectionName: String?): Result<Unit> {
        return runCatching {
            db.runTransaction { transaction ->
                val postSaveRef = db.collection(FirestoreCollections.PHOTO_POSTS)
                    .document(postId)
                    .collection(FirestoreCollections.SAVES)
                    .document(userId)
                val userSaveRef = db.collection(FirestoreCollections.USERS)
                    .document(userId)
                    .collection(FirestoreCollections.SAVED_POSTS)
                    .document(postId)
                val postRef = db.collection(FirestoreCollections.PHOTO_POSTS).document(postId)
                val userRef = db.collection(FirestoreCollections.USERS).document(userId)

                val alreadySaved = transaction.get(postSaveRef).exists()
                if (alreadySaved) return@runTransaction Unit

                transaction.set(
                    postSaveRef,
                    PostSaveDocument(
                        userId = userId,
                        postId = postId,
                        createdAt = Timestamp.now(),
                        collectionName = collectionName
                    )
                )
                transaction.set(
                    userSaveRef,
                    UserSavedPostDocument(
                        postId = postId,
                        createdAt = Timestamp.now(),
                        collectionName = collectionName
                    )
                )
                transaction.update(postRef, "saveCount", FieldValue.increment(1))
                transaction.set(userRef, mapOf("savedCount" to FieldValue.increment(1)), SetOptions.merge())
                Unit
            }.awaitResult()
        }
    }

    suspend fun unsavePost(userId: String, postId: String): Result<Unit> {
        return runCatching {
            db.runTransaction { transaction ->
                val postSaveRef = db.collection(FirestoreCollections.PHOTO_POSTS)
                    .document(postId)
                    .collection(FirestoreCollections.SAVES)
                    .document(userId)
                val userSaveRef = db.collection(FirestoreCollections.USERS)
                    .document(userId)
                    .collection(FirestoreCollections.SAVED_POSTS)
                    .document(postId)
                val postRef = db.collection(FirestoreCollections.PHOTO_POSTS).document(postId)
                val userRef = db.collection(FirestoreCollections.USERS).document(userId)

                val alreadySaved = transaction.get(postSaveRef).exists()
                if (!alreadySaved) return@runTransaction Unit

                transaction.delete(postSaveRef)
                transaction.delete(userSaveRef)
                transaction.update(postRef, "saveCount", FieldValue.increment(-1))
                transaction.set(userRef, mapOf("savedCount" to FieldValue.increment(-1)), SetOptions.merge())
                Unit
            }.awaitResult()
        }
    }

    suspend fun isPostLikedByUser(userId: String, postId: String): Boolean {
        val doc = db.collection(FirestoreCollections.PHOTO_POSTS)
            .document(postId)
            .collection(FirestoreCollections.LIKES)
            .document(userId)
            .get()
            .awaitResult()
        return doc.exists()
    }

    suspend fun isPostSavedByUser(userId: String, postId: String): Boolean {
        val doc = db.collection(FirestoreCollections.PHOTO_POSTS)
            .document(postId)
            .collection(FirestoreCollections.SAVES)
            .document(userId)
            .get()
            .awaitResult()
        return doc.exists()
    }

    suspend fun softDeletePost(postId: String, userId: String): Result<Unit> {
        return runCatching {
            val postRef = db.collection(FirestoreCollections.PHOTO_POSTS).document(postId)
            val userRef = db.collection(FirestoreCollections.USERS).document(userId)
            db.runTransaction { transaction ->
                val postSnap = transaction.get(postRef)
                val post = postSnap.toObject(PhotoPostDocument::class.java)
                    ?: return@runTransaction Unit
                if (post.status == "deleted") return@runTransaction Unit

                transaction.update(
                    postRef,
                    mapOf(
                        "status" to "deleted",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(userRef, mapOf("postCount" to FieldValue.increment(-1)), SetOptions.merge())
                if (post.visibility == "group" && !post.groupId.isNullOrBlank()) {
                    val groupRef = db.collection(FirestoreCollections.GROUPS).document(post.groupId)
                    transaction.set(groupRef, mapOf("postCount" to FieldValue.increment(-1)), SetOptions.merge())
                }
                Unit
            }.awaitResult()
        }
    }

    suspend fun updatePost(
        postId: String,
        title: String,
        description: String,
        visibility: String,
        tags: List<String>
    ): Result<Unit> {
        return runCatching {
            db.collection(FirestoreCollections.PHOTO_POSTS)
                .document(postId)
                .update(
                    mapOf(
                        "title" to title,
                        "description" to description,
                        "visibility" to visibility,
                        "tags" to tags,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .awaitResult()
        }
    }

    suspend fun getLikedPosts(userId: String): Result<List<PhotoPostDocument>> {
        return runCatching {
            val likedDocs = db.collection(FirestoreCollections.USERS)
                .document(userId)
                .collection(FirestoreCollections.LIKED_POSTS)
                .get()
                .awaitResult()
                .documents

            val postIds = likedDocs.map { it.id }
            postIds.mapNotNull { postId ->
                db.collection(FirestoreCollections.PHOTO_POSTS)
                    .document(postId)
                    .get()
                    .awaitResult()
                    .toObject(PhotoPostDocument::class.java)
            }.filterNot { it.status == "deleted" }.sortedByNewest()
        }
    }

    suspend fun getSavedPosts(userId: String): Result<List<PhotoPostDocument>> {
        return runCatching {
            val savedDocs = db.collection(FirestoreCollections.USERS)
                .document(userId)
                .collection(FirestoreCollections.SAVED_POSTS)
                .get()
                .awaitResult()
                .documents

            val postIds = savedDocs.map { it.id }
            postIds.mapNotNull { postId ->
                db.collection(FirestoreCollections.PHOTO_POSTS)
                    .document(postId)
                    .get()
                    .awaitResult()
                    .toObject(PhotoPostDocument::class.java)
            }.filterNot { it.status == "deleted" }.sortedByNewest()
        }
    }

    suspend fun reportPost(
        postId: String,
        reporterId: String?,
        reason: String,
        description: String? = null
    ): Result<String> {
        return runCatching {
            val reportRef = db.collection(FirestoreCollections.REPORTS).document()
            val reportId = reportRef.id
            val postRef = db.collection(FirestoreCollections.PHOTO_POSTS).document(postId)

            val payload = mapOf(
                "reportId" to reportId,
                "postId" to postId,
                "reporterId" to reporterId,
                "reason" to reason,
                "description" to description,
                "createdAt" to Timestamp.now(),
                "status" to "pending"
            )
            val batch = db.batch()
            batch.set(reportRef, payload)
            batch.update(postRef, "reportCount", FieldValue.increment(1))
            batch.commit().awaitResult()
            reportId
        }
    }

    private suspend fun uploadAllImages(
        authorId: String,
        postId: String,
        uris: List<Uri>
    ): List<String> = coroutineScope {
        uris.mapIndexed { index, uri ->
            async {
                val ref = storage.reference
                    .child("travelshare/posts/$authorId/$postId/$index.jpg")
                ref.putFile(uri).awaitResult()
                ref.downloadUrl.awaitResult().toString()
            }
        }.awaitAll()
    }
}

private fun List<PhotoPostDocument>.sortedByNewest(): List<PhotoPostDocument> {
    return sortedWith(
        compareByDescending<PhotoPostDocument> { it.createdAt?.seconds ?: 0L }
            .thenByDescending { it.createdAt?.nanoseconds ?: 0 }
    )
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { ex ->
        if (cont.isActive) cont.resumeWithException(ex)
    }
}
