package com.example.traveling.data.repository

import android.net.Uri
import com.example.traveling.data.model.FirestoreCollections
import com.example.traveling.data.model.NotificationSettingsDocument
import com.example.traveling.data.model.User
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    suspend fun createUserDocumentIfMissing(
        userId: String,
        displayName: String,
        email: String,
        avatarUrl: String? = null,
        bio: String = ""
    ) {
        val userRef = db.collection(FirestoreCollections.USERS).document(userId)
        val existing = userRef.get().awaitResult()

        if (existing.exists()) {
            // si le profil existe déjà, on met seulement à jour les infos utiles
            val updates = mutableMapOf<String, Any>(
                "lastLoginAt" to FieldValue.serverTimestamp()
            )
            if (!avatarUrl.isNullOrBlank()) updates["avatarUrl"] = avatarUrl
            if (bio.isNotBlank()) updates["bio"] = bio
            if (updates.size > 1) {
                userRef.update(updates).awaitResult()
            } else {
                updateLastLoginAt(userId)
            }
            return
        }

        val batch = db.batch()

        val userPayload = hashMapOf(
            "userId" to userId,
            "displayName" to displayName,
            "email" to email,
            "avatarUrl" to avatarUrl,
            "bio" to bio,
            "homeCity" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "lastLoginAt" to FieldValue.serverTimestamp(),
            "postCount" to 0,
            "likedCount" to 0,
            "savedCount" to 0,
            "groupCount" to 0,
            "isAnonymousUpgraded" to false
        )
        batch.set(userRef, userPayload)

        val notifRef = userRef
            .collection(FirestoreCollections.NOTIFICATION_SETTINGS)
            .document(FirestoreCollections.DEFAULT_SETTINGS_DOC)
        // chaque utilisateur reçoit ses préférences de notification par défaut
        batch.set(notifRef, NotificationSettingsDocument())

        batch.commit().awaitResult()
    }

    suspend fun uploadUserAvatar(userId: String, localUri: Uri): String {
        val ref = storage.reference.child("users/$userId/avatar.jpg")
        ref.putFile(localUri).awaitResult()
        return ref.downloadUrl.awaitResult().toString()
    }

    suspend fun updateLastLoginAt(userId: String) {
        db.collection(FirestoreCollections.USERS)
            .document(userId)
            .update("lastLoginAt", FieldValue.serverTimestamp())
            .awaitResult()
    }

    suspend fun getUser(userId: String): User? {
        val snapshot = db.collection(FirestoreCollections.USERS)
            .document(userId)
            .get()
            .awaitResult()
        return snapshot.toObject(User::class.java)
    }

    suspend fun getUsers(userIds: List<String>): List<User> {
        return userIds.distinct().mapNotNull { getUser(it) }
    }

    fun observeUser(
        userId: String,
        onChanged: (User?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection(FirestoreCollections.USERS)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                onChanged(snapshot?.toObject(User::class.java))
            }
    }

    @Suppress("unused")
    fun currentUserId(): String? = auth.currentUser?.uid
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { ex ->
        if (cont.isActive) cont.resumeWithException(ex)
    }
}
