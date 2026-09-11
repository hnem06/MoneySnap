package com.devpro58.hnem06.moneysnap.data.remote.firestore

import com.devpro58.hnem06.moneysnap.data.remote.firebase.awaitTask
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Removes everything stored under `users/{uid}` when an account is deleted.
 *
 * Firestore has no recursive delete from a client, so each subcollection is paged and batched
 * explicitly. A Cloud Function on an Auth delete trigger would be more robust, but it needs the
 * Blaze plan; this runs on Spark and is what Play's deletion requirement asks for today.
 */
@Singleton
class FirestoreAccountDeletionSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun deleteAllUserData(userId: String) {
        val userDocument = firestore.collection("users").document(userId)

        // Subcollections are not removed by deleting their parent document — they would become
        // unreachable orphans that still count against storage and still exist in the console.
        SUBCOLLECTIONS.forEach { name ->
            deleteCollection(userDocument.collection(name))
        }

        userDocument.delete().awaitTask()
    }

    private suspend fun deleteCollection(collection: com.google.firebase.firestore.CollectionReference) {
        while (true) {
            val page: Query = collection.limit(BATCH_SIZE.toLong())
            val snapshot = page.get().awaitTask()
            if (snapshot.isEmpty) return

            val batch = firestore.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit().awaitTask()

            // A full page may mean there are more; a short one is definitely the last.
            if (snapshot.size() < BATCH_SIZE) return
        }
    }

    private companion object {
        val SUBCOLLECTIONS = listOf("expenses", "paymentMethods")

        /** Firestore caps a write batch at 500 operations. */
        const val BATCH_SIZE = 500
    }
}
