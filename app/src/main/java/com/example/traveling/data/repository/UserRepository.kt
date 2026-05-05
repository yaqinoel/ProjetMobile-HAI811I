package com.example.traveling.data.repository

import com.example.traveling.data.model.FirestoreCollections
import com.example.traveling.data.model.NotificationSettingsDocument
import com.example.traveling.data.model.User
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun createUserDocumentIfMissing(
        userId: String,
        displayName: String,
        email: String
    ) {
        val userRef = db.collection(FirestoreCollections.USERS).document(userId)
        val existing = userRef.get().awaitResult()

        if (existing.exists()) {
            updateLastLoginAt(userId)
            return
        }

        val batch = db.batch()

        val userPayload = hashMapOf(
            "userId" to userId,
            "displayName" to displayName,
            "email" to email,
            "avatarUrl" to null,
            "bio" to "",
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
        batch.set(notifRef, NotificationSettingsDocument())

        batch.commit().awaitResult()
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
