package com.example.traveling.data.repository

import com.example.traveling.data.model.Attraction
import com.example.traveling.data.model.Destination
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Repository pour accéder aux données Firestore (destinations & attractions).
 */
class TravelRepository {

    private val db = FirebaseFirestore.getInstance()

    /** Flux temps-réel de toutes les destinations. */
    fun getDestinations(): Flow<List<Destination>> = callbackFlow {
        val listener = db.collection("destinations")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Destination::class.java)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /** Flux temps-réel des attractions pour une destination donnée. */
    fun getAttractionsByDestination(destinationId: String): Flow<List<Attraction>> = callbackFlow {
        val listener = db.collection("attractions")
            .whereEqualTo("destinationId", destinationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Attraction::class.java)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /** Flux temps-réel de TOUTES les attractions. */
    fun getAllAttractions(): Flow<List<Attraction>> = callbackFlow {
        val listener = db.collection("attractions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Attraction::class.java)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }
}
