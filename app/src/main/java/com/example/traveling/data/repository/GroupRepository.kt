package com.example.traveling.data.repository

import com.example.traveling.data.model.FirestoreCollections
import com.example.traveling.data.model.GroupDocument
import com.example.traveling.data.model.GroupMemberDocument
import com.example.traveling.data.model.UserJoinedGroupDocument
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class CreateGroupInput(
    val name: String,
    val description: String,
    val ownerId: String,
    val ownerName: String,
    val ownerAvatarUrl: String?,
    val visibility: String
)

class GroupRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun createGroup(input: CreateGroupInput): Result<String> {
        return runCatching {
            val now = Timestamp.now()
            val groupRef = db.collection(FirestoreCollections.GROUPS).document()
            val groupId = groupRef.id
            val ownerMemberRef = groupRef.collection(FirestoreCollections.MEMBERS).document(input.ownerId)
            val userJoinedRef = db.collection(FirestoreCollections.USERS)
                .document(input.ownerId)
                .collection(FirestoreCollections.JOINED_GROUPS)
                .document(groupId)
            val userRef = db.collection(FirestoreCollections.USERS).document(input.ownerId)

            val group = GroupDocument(
                groupId = groupId,
                name = input.name,
                description = input.description,
                coverImageUrl = null,
                ownerId = input.ownerId,
                ownerName = input.ownerName,
                memberCount = 1,
                postCount = 0,
                visibility = input.visibility,
                createdAt = now,
                updatedAt = now
            )

            val ownerMember = GroupMemberDocument(
                userId = input.ownerId,
                displayName = input.ownerName,
                avatarUrl = input.ownerAvatarUrl,
                role = "owner",
                joinedAt = now
            )

            val joinedGroup = UserJoinedGroupDocument(
                groupId = groupId,
                name = input.name,
                role = "owner",
                joinedAt = now
            )

            val batch = db.batch()
            // création du groupe et ajout du créateur comme premier membre
            batch.set(groupRef, group)
            batch.set(ownerMemberRef, ownerMember)
            batch.set(userJoinedRef, joinedGroup)
            batch.set(userRef, mapOf("groupCount" to FieldValue.increment(1)), SetOptions.merge())
            batch.commit().awaitResult()

            groupId
        }
    }

    fun observeMyGroups(
        userId: String,
        onChanged: (List<GroupDocument>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection(FirestoreCollections.USERS)
            .document(userId)
            .collection(FirestoreCollections.JOINED_GROUPS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val groupIds = snapshot?.documents?.map { it.id }.orEmpty()
                fetchGroupsByIds(groupIds, onChanged, onError)
            }
    }

    fun observeGroup(
        groupId: String,
        onChanged: (GroupDocument?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection(FirestoreCollections.GROUPS)
            .document(groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                onChanged(snapshot?.toObject(GroupDocument::class.java))
            }
    }

    fun observeDiscoverGroups(
        userId: String,
        onChanged: (List<GroupDocument>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        var joinedIds: Set<String> = emptySet()
        var allGroups: List<GroupDocument> = emptyList()

        fun emit() {
            // on affiche seulement les groupes que l'utilisateur n'a pas encore rejoints
            onChanged(
                allGroups
                    .filterNot { joinedIds.contains(it.groupId) }
                    .sortedByDescending { it.updatedAt?.seconds ?: 0L }
            )
        }

        val joinedListener = db.collection(FirestoreCollections.USERS)
            .document(userId)
            .collection(FirestoreCollections.JOINED_GROUPS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                joinedIds = snapshot?.documents?.map { it.id }?.toSet().orEmpty()
                emit()
            }

        val groupsListener = db.collection(FirestoreCollections.GROUPS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                allGroups = snapshot?.documents
                    ?.mapNotNull { it.toObject(GroupDocument::class.java) }
                    .orEmpty()
                emit()
            }

        return ListenerRegistration {
            joinedListener.remove()
            groupsListener.remove()
        }
    }

    suspend fun joinGroup(userId: String, groupId: String): Result<Unit> {
        return runCatching {
            db.runTransaction { transaction ->
                val groupRef = db.collection(FirestoreCollections.GROUPS).document(groupId)
                val groupMemberRef = groupRef.collection(FirestoreCollections.MEMBERS).document(userId)
                val userJoinedRef = db.collection(FirestoreCollections.USERS)
                    .document(userId)
                    .collection(FirestoreCollections.JOINED_GROUPS)
                    .document(groupId)
                val userRef = db.collection(FirestoreCollections.USERS).document(userId)

                val memberSnap = transaction.get(groupMemberRef)
                if (memberSnap.exists()) return@runTransaction Unit

                val groupSnap = transaction.get(groupRef)
                val group = groupSnap.toObject(GroupDocument::class.java)
                    ?: throw IllegalStateException("Groupe introuvable")

                val userSnap = transaction.get(userRef)
                val displayName = userSnap.getString("displayName").orEmpty().ifBlank { "Voyageur" }
                val avatarUrl = userSnap.getString("avatarUrl")

                transaction.set(
                    groupMemberRef,
                    GroupMemberDocument(
                        userId = userId,
                        displayName = displayName,
                        avatarUrl = avatarUrl,
                        role = "member",
                        joinedAt = Timestamp.now()
                    )
                )
                transaction.set(
                    userJoinedRef,
                    UserJoinedGroupDocument(
                        groupId = groupId,
                        name = group.name,
                        role = "member",
                        joinedAt = Timestamp.now()
                    )
                )
                transaction.update(groupRef, "memberCount", FieldValue.increment(1))
                transaction.set(userRef, mapOf("groupCount" to FieldValue.increment(1)), SetOptions.merge())
                Unit
            }.awaitResult()
        }
    }

    suspend fun leaveGroup(userId: String, groupId: String): Result<Unit> {
        return runCatching {
            db.runTransaction { transaction ->
                val groupRef = db.collection(FirestoreCollections.GROUPS).document(groupId)
                val groupMemberRef = groupRef.collection(FirestoreCollections.MEMBERS).document(userId)
                val userJoinedRef = db.collection(FirestoreCollections.USERS)
                    .document(userId)
                    .collection(FirestoreCollections.JOINED_GROUPS)
                    .document(groupId)
                val userRef = db.collection(FirestoreCollections.USERS).document(userId)

                val memberSnap = transaction.get(groupMemberRef)
                if (!memberSnap.exists()) return@runTransaction Unit

                transaction.delete(groupMemberRef)
                transaction.delete(userJoinedRef)
                transaction.update(groupRef, "memberCount", FieldValue.increment(-1))
                transaction.set(userRef, mapOf("groupCount" to FieldValue.increment(-1)), SetOptions.merge())
                Unit
            }.awaitResult()
        }
    }

    suspend fun getJoinedGroups(userId: String): Result<List<UserJoinedGroupDocument>> {
        return runCatching {
            db.collection(FirestoreCollections.USERS)
                .document(userId)
                .collection(FirestoreCollections.JOINED_GROUPS)
                .get()
                .awaitResult()
                .documents
                .mapNotNull { it.toObject(UserJoinedGroupDocument::class.java) }
                .sortedByDescending { it.joinedAt?.seconds ?: 0L }
        }
    }

    private fun fetchGroupsByIds(
        groupIds: List<String>,
        onChanged: (List<GroupDocument>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (groupIds.isEmpty()) {
            onChanged(emptyList())
            return
        }

        val tasks = groupIds.map { groupId ->
            db.collection(FirestoreCollections.GROUPS).document(groupId).get()
        }

        Tasks.whenAllComplete(tasks)
            .addOnSuccessListener {
                val groups = tasks.mapNotNull { task ->
                    if (!task.isSuccessful) {
                        null
                    } else {
                        task.result?.toObject(GroupDocument::class.java)
                    }
                }.sortedByDescending { it.updatedAt?.seconds ?: 0L }
                onChanged(groups)
            }
            .addOnFailureListener { ex ->
                onError(ex)
            }
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
