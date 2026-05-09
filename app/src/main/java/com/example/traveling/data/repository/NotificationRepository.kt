package com.example.traveling.data.repository

import com.example.traveling.data.model.NotificationDocument
import com.example.traveling.data.model.NotificationSettingsDocument
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class NotificationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val notificationsRef = db.collection("notifications")

    fun observeMyNotifications(
        userId: String,
        onChanged: (List<NotificationDocument>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return notificationsRef
            .whereEqualTo("receiverId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(NotificationDocument::class.java) }
                    onChanged(list)
                }
            }
    }

    fun observeNotificationSettings(
        userId: String,
        onChanged: (NotificationSettingsDocument) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection("users").document(userId)
            .collection("notificationSettings").document("default")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val settings = snapshot?.toObject(NotificationSettingsDocument::class.java)
                    ?: NotificationSettingsDocument() // Provide default if it doesn't exist
                onChanged(settings)
            }
    }

    suspend fun markAsRead(notificationId: String) {
        try {
            notificationsRef.document(notificationId).update("read", true).await()
            // Some models use "isRead", some use "read". Using isRead to match NotificationDocument:
            notificationsRef.document(notificationId).update("isRead", true).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun markAllAsRead(userId: String) {
        try {
            val unreadDocs = notificationsRef
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()
            
            val batch = db.batch()
            for (doc in unreadDocs.documents) {
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteNotification(notificationId: String) {
        try {
            notificationsRef.document(notificationId).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateNotificationSettings(userId: String, settings: NotificationSettingsDocument) {
        try {
            db.collection("users").document(userId)
                .collection("notificationSettings").document("default")
                .set(settings)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
