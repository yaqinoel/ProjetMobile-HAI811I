package com.example.traveling.data.repository

import android.net.Uri
import com.example.traveling.data.model.FirestoreCollections
import com.example.traveling.data.model.PhotoCommentDocument
import com.example.traveling.data.model.PhotoPostDocument
import com.example.traveling.data.model.PostLikeDocument
import com.example.traveling.data.model.PostSaveDocument
import com.example.traveling.data.model.TravelShareAttractionDocument
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
    val localVoiceNoteUri: Uri?,
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
    val isLinkedToTravelPath: Boolean,
    val travelPathCost: Int? = null,
    val travelPathDurationMinutes: Int? = null,
    val travelPathEffortLevel: Int? = null,
    val travelPathOpenHours: String? = null,
    val travelPathClosedDay: String? = null,
    val travelPathWeatherType: String? = null,
    val travelPathBestTimeSlots: List<String> = emptyList()
)

class PhotoPostRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val notificationRepository: NotificationRepository = NotificationRepository(),
    private val travelRepository: TravelRepository = TravelRepository()
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
            val voiceNoteUrl = input.localVoiceNoteUri?.let { uri ->
                uploadVoiceNote(
                    authorId = input.authorId,
                    postId = postId,
                    uri = uri
                )
            }

            val now = Timestamp.now()
            val visibleLat = input.displayLatitude ?: input.rawLatitude
            val visibleLng = input.displayLongitude ?: input.rawLongitude
            val shouldLinkToTravelPath = input.isLinkedToTravelPath && input.visibility == "public"
            val travelPathDestination = if (shouldLinkToTravelPath) {
                travelRepository.ensureTravelPathDestinationForPost(
                    city = input.city,
                    country = input.country,
                    lat = visibleLat,
                    lng = visibleLng,
                    imageUrl = uploadedUrls.firstOrNull(),
                    sourcePostId = postId,
                    userId = input.authorId
                )
            } else {
                null
            }
            val post = PhotoPostDocument(
                postId = postId,
                authorId = input.authorId,
                authorName = input.authorName,
                authorAvatarUrl = input.authorAvatarUrl,
                title = input.title,
                description = input.description,
                imageUrls = uploadedUrls,
                voiceNoteUrl = voiceNoteUrl,
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
                isLinkedToTravelPath = shouldLinkToTravelPath && travelPathDestination != null,
                travelPathPlaceId = if (shouldLinkToTravelPath && travelPathDestination != null) "photo_$postId" else null,
                travelPathDestinationId = travelPathDestination?.id,
                travelPathDestinationName = travelPathDestination?.name,
                travelPathSource = travelPathDestination?.source,
                status = "published"
            )

            val userRef = db.collection(FirestoreCollections.USERS).document(input.authorId)
            val batch = db.batch()
            batch.set(postRef, post)
            buildTravelShareAttraction(
                post = post,
                costOverride = input.travelPathCost,
                durationMinutesOverride = input.travelPathDurationMinutes,
                effortLevelOverride = input.travelPathEffortLevel,
                openHoursOverride = input.travelPathOpenHours,
                closedDayOverride = input.travelPathClosedDay,
                weatherTypeOverride = input.travelPathWeatherType,
                bestTimeSlotsOverride = input.travelPathBestTimeSlots
            )?.let { attraction ->
                batch.set(
                    db.collection(FirestoreCollections.TRAVEL_SHARE_ATTRACTIONS).document(attraction.id),
                    attraction,
                    SetOptions.merge()
                )
            }
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
            .whereEqualTo("visibility", "public")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents
                    ?.mapNotNull { it.toObject(PhotoPostDocument::class.java) }
                    ?.filter { it.status == "published" }
                    ?.sortedByNewest()
                    ?: emptyList()
                onChanged(posts)
            }
    }

    fun observeVisiblePublishedPosts(
        userId: String?,
        onChanged: (List<PhotoPostDocument>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        if (userId.isNullOrBlank()) {
            return observePublicPublishedPosts(onChanged, onError)
        }

        var publicPosts: List<PhotoPostDocument> = emptyList()
        var publicLoaded = false
        var joinedGroupsLoaded = false
        var currentGroupIds: Set<String> = emptySet()
        val groupPostsByGroupId = mutableMapOf<String, List<PhotoPostDocument>>()
        val groupListeners = mutableListOf<ListenerRegistration>()

        fun emit() {
            if (!publicLoaded || !joinedGroupsLoaded) return
            if (!groupPostsByGroupId.keys.containsAll(currentGroupIds)) return

            val posts = (publicPosts + groupPostsByGroupId.values.flatten())
                .distinctBy { it.postId }
                .sortedByNewest()
            onChanged(posts)
        }

        val publicListener = observePublicPublishedPosts(
            onChanged = { posts ->
                publicPosts = posts
                publicLoaded = true
                emit()
            },
            onError = onError
        )

        val joinedGroupsListener = db.collection(FirestoreCollections.USERS)
            .document(userId)
            .collection(FirestoreCollections.JOINED_GROUPS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                groupListeners.forEach { it.remove() }
                groupListeners.clear()
                groupPostsByGroupId.clear()

                val groupIds = snapshot?.documents?.map { it.id }.orEmpty()
                currentGroupIds = groupIds.toSet()
                joinedGroupsLoaded = true
                if (groupIds.isEmpty()) {
                    emit()
                    return@addSnapshotListener
                }

                groupIds.forEach { groupId ->
                    val listener = observeGroupPublishedPosts(
                        groupId = groupId,
                        onChanged = { posts ->
                            groupPostsByGroupId[groupId] = posts
                            emit()
                        },
                        onError = onError
                    )
                    groupListeners.add(listener)
                }
            }

        return ListenerRegistration {
            publicListener.remove()
            joinedGroupsListener.remove()
            groupListeners.forEach { it.remove() }
            groupListeners.clear()
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

    fun observePublicPublishedPostsByAuthor(
        authorId: String,
        onChanged: (List<PhotoPostDocument>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection(FirestoreCollections.PHOTO_POSTS)
            .whereEqualTo("authorId", authorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents
                    ?.mapNotNull { it.toObject(PhotoPostDocument::class.java) }
                    ?.filter { it.status == "published" && it.visibility == "public" }
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
            .whereEqualTo("groupId", groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents
                    ?.mapNotNull { it.toObject(PhotoPostDocument::class.java) }
                    ?.filter { it.status == "published" && it.visibility == "group" }
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

    suspend fun getPostOnce(postId: String): Result<PhotoPostDocument> {
        return runCatching {
            db.collection(FirestoreCollections.PHOTO_POSTS)
                .document(postId)
                .get()
                .awaitResult()
                .toObject(PhotoPostDocument::class.java)
                ?: error("Post introuvable")
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

    suspend fun getLikedPostIds(userId: String): Set<String> {
        return db.collection(FirestoreCollections.USERS)
            .document(userId)
            .collection(FirestoreCollections.LIKED_POSTS)
            .get()
            .awaitResult()
            .documents
            .map { it.id }
            .toSet()
    }

    suspend fun getSavedPostIds(userId: String): Set<String> {
        return db.collection(FirestoreCollections.USERS)
            .document(userId)
            .collection(FirestoreCollections.SAVED_POSTS)
            .get()
            .awaitResult()
            .documents
            .map { it.id }
            .toSet()
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
                transaction.set(
                    db.collection(FirestoreCollections.TRAVEL_SHARE_ATTRACTIONS).document("photo_${post.postId}"),
                    mapOf(
                        "status" to "inactive",
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
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
        tags: List<String>,
        isLinkedToTravelPath: Boolean
    ): Result<Unit> {
        return runCatching {
            val existingPost = db.collection(FirestoreCollections.PHOTO_POSTS)
                .document(postId)
                .get()
                .awaitResult()
                .toObject(PhotoPostDocument::class.java)

            val shouldLinkToTravelPath = isLinkedToTravelPath && visibility.lowercase() == "public"
            val visibleLat = existingPost?.displayLatitude ?: existingPost?.latitude ?: existingPost?.rawLatitude
            val visibleLng = existingPost?.displayLongitude ?: existingPost?.longitude ?: existingPost?.rawLongitude
            val travelPathDestination = if (shouldLinkToTravelPath && existingPost != null) {
                travelRepository.ensureTravelPathDestinationForPost(
                    city = existingPost.city,
                    country = existingPost.country,
                    lat = visibleLat,
                    lng = visibleLng,
                    imageUrl = existingPost.imageUrls.firstOrNull(),
                    sourcePostId = existingPost.postId,
                    userId = existingPost.authorId
                )
            } else {
                null
            }
            val updatedPostForPlace = existingPost?.copy(
                title = title,
                description = description,
                tags = tags,
                visibility = visibility,
                isLinkedToTravelPath = shouldLinkToTravelPath && travelPathDestination != null,
                travelPathPlaceId = if (shouldLinkToTravelPath && travelPathDestination != null) "photo_$postId" else null,
                travelPathDestinationId = travelPathDestination?.id,
                travelPathDestinationName = travelPathDestination?.name,
                travelPathSource = travelPathDestination?.source
            )
            val updateValues = mutableMapOf<String, Any?>(
                "title" to title,
                "description" to description,
                "visibility" to visibility,
                "tags" to tags,
                "isLinkedToTravelPath" to (shouldLinkToTravelPath && travelPathDestination != null),
                "travelPathPlaceId" to if (shouldLinkToTravelPath && travelPathDestination != null) "photo_$postId" else null,
                "travelPathDestinationId" to travelPathDestination?.id,
                "travelPathDestinationName" to travelPathDestination?.name,
                "travelPathSource" to travelPathDestination?.source,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val attraction = updatedPostForPlace?.let { buildTravelShareAttraction(it) }
            if (attraction != null) {
                db.batch().apply {
                    update(
                        db.collection(FirestoreCollections.PHOTO_POSTS).document(postId),
                        updateValues
                    )
                    set(
                        db.collection(FirestoreCollections.TRAVEL_SHARE_ATTRACTIONS).document(attraction.id),
                        attraction,
                        SetOptions.merge()
                    )
                }.commit().awaitResult()
                return@runCatching
            }

            db.batch().apply {
                update(
                    db.collection(FirestoreCollections.PHOTO_POSTS).document(postId),
                    updateValues
                )
                set(
                    db.collection(FirestoreCollections.TRAVEL_SHARE_ATTRACTIONS).document("photo_$postId"),
                    mapOf(
                        "status" to "inactive",
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
            }.commit().awaitResult()
        }
    }

    private fun buildTravelShareAttraction(
        post: PhotoPostDocument,
        costOverride: Int? = null,
        durationMinutesOverride: Int? = null,
        effortLevelOverride: Int? = null,
        openHoursOverride: String? = null,
        closedDayOverride: String? = null,
        weatherTypeOverride: String? = null,
        bestTimeSlotsOverride: List<String> = emptyList()
    ): TravelShareAttractionDocument? {
        if (!post.isLinkedToTravelPath || post.visibility != "public" || post.status != "published") return null
        val destinationId = post.travelPathDestinationId ?: return null
        val destinationName = post.travelPathDestinationName ?: post.city ?: return null
        val lat = post.displayLatitude ?: post.latitude ?: post.rawLatitude ?: return null
        val lng = post.displayLongitude ?: post.longitude ?: post.rawLongitude ?: return null
        val name = post.locationName.ifBlank { post.title }.ifBlank { return null }
        val now = Timestamp.now()
        val attractionType = mapPlaceTypeToAttractionType(post.placeType, post.tags)

        return TravelShareAttractionDocument(
            id = "photo_${post.postId}",
            destinationId = destinationId,
            name = name,
            type = attractionType,
            cost = costOverride?.coerceAtLeast(0)
                ?: estimateTravelShareCost(attractionType, post.placeType, post.tags),
            duration = durationMinutesOverride?.takeIf { it > 0 } ?: 45,
            rating = 4.2 + minOf(post.likeCount, 50) / 100.0,
            description = post.description.ifBlank { post.title },
            imageUrl = post.imageUrls.firstOrNull().orEmpty(),
            lat = lat,
            lng = lng,
            openHours = openHoursOverride?.takeIf { it.isNotBlank() } ?: "Horaires non renseignés",
            closedDay = closedDayOverride.orEmpty(),
            effortLevel = effortLevelOverride?.takeIf { it in 1..5 } ?: 1,
            weatherType = weatherTypeOverride?.takeIf { it in setOf("indoor", "outdoor", "both") } ?: "both",
            bestTimeSlots = bestTimeSlotsOverride.ifEmpty { listOf("apres-midi") },
            tags = (post.tags + "TravelShare").distinct(),
            imageUrls = post.imageUrls,
            source = "travelshare",
            sourcePostId = post.postId,
            destinationName = destinationName,
            authorId = post.authorId,
            createdAt = post.createdAt ?: now,
            updatedAt = now,
            status = "active"
        )
    }

    private fun mapPlaceTypeToAttractionType(placeType: String, tags: List<String>): String {
        val terms = (listOf(placeType) + tags).joinToString(" ").lowercase()
        return when {
            terms.contains("museum") || terms.contains("musée") || terms.contains("culture") || terms.contains("cultural") -> "Culture"
            terms.contains("monument") || terms.contains("architecture") || terms.contains("street") || terms.contains("rue") -> "Monument"
            terms.contains("nature") || terms.contains("park") || terms.contains("parc") || terms.contains("beach") ||
                terms.contains("mountain") || terms.contains("lake") || terms.contains("forest") -> "Nature"
            terms.contains("restaurant") || terms.contains("food") || terms.contains("gastronomie") ||
                terms.contains("cafe") || terms.contains("café") -> "Gastronomie"
            terms.contains("shop") || terms.contains("shopping") || terms.contains("market") || terms.contains("magasin") -> "Shopping"
            terms.contains("sport") -> "Loisirs"
            terms.contains("nightlife") || terms.contains("soir") || terms.contains("bar") -> "Loisirs"
            terms.contains("leisure") || terms.contains("loisirs") -> "Loisirs"
            else -> "Photo"
        }
    }

    private fun estimateTravelShareCost(type: String, placeType: String, tags: List<String>): Int {
        val terms = (listOf(type, placeType) + tags).joinToString(" ").lowercase()
        return when {
            terms.contains("restaurant") || terms.contains("food") || terms.contains("gastronomie") ||
                terms.contains("cafe") || terms.contains("café") -> 25
            terms.contains("shop") || terms.contains("shopping") || terms.contains("market") || terms.contains("magasin") -> 20
            terms.contains("museum") || terms.contains("musée") || terms.contains("culture") -> 12
            terms.contains("monument") || terms.contains("architecture") -> 10
            terms.contains("sport") -> 15
            terms.contains("nightlife") || terms.contains("bar") -> 30
            terms.contains("leisure") || terms.contains("loisirs") -> 18
            terms.contains("nature") || terms.contains("park") || terms.contains("parc") ||
                terms.contains("beach") || terms.contains("mountain") || terms.contains("lake") || terms.contains("forest") -> 0
            else -> 5
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

    private suspend fun uploadVoiceNote(
        authorId: String,
        postId: String,
        uri: Uri
    ): String {
        val ref = storage.reference
            .child("travelshare/posts/$authorId/$postId/voice_note.m4a")
        ref.putFile(uri).awaitResult()
        return ref.downloadUrl.awaitResult().toString()
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
