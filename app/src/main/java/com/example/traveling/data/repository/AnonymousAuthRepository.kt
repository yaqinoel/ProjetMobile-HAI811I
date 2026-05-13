package com.example.traveling.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.example.traveling.data.repository.UserRepository


class AnonymousAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userRepository: UserRepository = UserRepository()
) {
    suspend fun signInAnonymouslyIfNeeded(): Result<String> {
        return runCatching {
            val current = auth.currentUser
            if (current != null) {
                userRepository.createUserDocumentIfMissing(
                    userId = current.uid,
                    displayName = if (current.isAnonymous) {
                        "Voyageur anonyme"
                    } else {
                        current.displayName.orEmpty().ifBlank { "Voyageur" }
                    },
                    email = current.email.orEmpty()
                )
                return@runCatching current.uid
            }

            auth.signInAnonymously().awaitResult()

            val anonymousUser = auth.currentUser ?: error("Anonymous login failed")
            userRepository.createUserDocumentIfMissing(
                userId = anonymousUser.uid,
                displayName = "Voyageur anonyme",
                email = ""
            )

            anonymousUser.uid
        }
    }
}

private suspend fun <T> Task<T>.awaitResult(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener { ex ->
            if (cont.isActive) cont.resumeWithException(ex)
        }
    }
