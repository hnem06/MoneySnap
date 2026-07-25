package com.devpro58.hnem06.moneysnap.data.remote.firestore

import com.devpro58.hnem06.moneysnap.data.local.entity.PaymentMethodEntity
import com.devpro58.hnem06.moneysnap.data.remote.firebase.awaitTask
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Singleton
class FirestorePaymentMethodSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun observePaymentMethods(userId: String): Flow<List<PaymentMethodEntity>> = callbackFlow {
        val registration = collection(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val methods = snapshot?.documents.orEmpty().mapNotNull { it.toEntity(userId) }
            trySend(methods)
        }
        awaitClose { registration.remove() }
    }

    suspend fun getPaymentMethods(userId: String): List<PaymentMethodEntity> =
        collection(userId).get().awaitTask().documents.mapNotNull { it.toEntity(userId) }

    suspend fun upsertPaymentMethod(method: PaymentMethodEntity) {
        upsertPaymentMethods(method.userId, listOf(method))
    }

    suspend fun upsertPaymentMethods(userId: String, methods: List<PaymentMethodEntity>) {
        val batch = firestore.batch()
        batch.set(userDocument(userId), initializedData(), SetOptions.merge())
        methods.forEach { method ->
            require(method.userId == userId) { "Payment method belongs to another user" }
            batch.set(
                collection(userId).document(method.id),
                mapOf(
                    "id" to method.id,
                    "userId" to method.userId,
                    "name" to method.name,
                    "createdAtMillis" to method.createdAtMillis,
                    "updatedAtMillis" to method.updatedAtMillis
                )
            )
        }
        batch.commit().awaitTask()
    }

    suspend fun deletePaymentMethod(userId: String, methodId: String) {
        val batch = firestore.batch()
        batch.set(userDocument(userId), initializedData(), SetOptions.merge())
        batch.delete(collection(userId).document(methodId))
        batch.commit().awaitTask()
    }

    suspend fun markSyncInitialized(userId: String) {
        userDocument(userId).set(initializedData(), SetOptions.merge()).awaitTask()
    }

    /** Atomically elects one device and uploads its legacy Room-only methods. */
    suspend fun migrateLegacyMethods(
        userId: String,
        methods: List<PaymentMethodEntity>
    ): Boolean =
        firestore.runTransaction { transaction ->
            val userDocument = userDocument(userId)
            if (transaction.get(userDocument).getBoolean(SYNC_INITIALIZED_FIELD) == true) {
                false
            } else {
                transaction.set(userDocument, initializedData(), SetOptions.merge())
                methods.forEach { method ->
                    require(method.userId == userId) { "Payment method belongs to another user" }
                    transaction.set(
                        collection(userId).document(method.id),
                        mapOf(
                            "id" to method.id,
                            "userId" to method.userId,
                            "name" to method.name,
                            "createdAtMillis" to method.createdAtMillis,
                            "updatedAtMillis" to method.updatedAtMillis
                        )
                    )
                }
                true
            }
        }.awaitTask()

    private fun collection(userId: String) = firestore.collection("users")
        .document(userId)
        .collection("paymentMethods")

    private fun userDocument(userId: String) = firestore.collection("users").document(userId)

    private fun initializedData() = mapOf(
        SYNC_INITIALIZED_FIELD to true,
        "paymentMethodsSyncUpdatedAtMillis" to System.currentTimeMillis()
    )

    private fun DocumentSnapshot.toEntity(userId: String): PaymentMethodEntity? {
        val name = getString("name")?.takeIf { it.isNotBlank() } ?: return null
        return PaymentMethodEntity(
            id = getString("id") ?: id,
            userId = userId,
            name = name,
            builtInKey = null,
            createdAtMillis = getLong("createdAtMillis") ?: 0L,
            updatedAtMillis = getLong("updatedAtMillis") ?: 0L
        )
    }

    private companion object {
        const val SYNC_INITIALIZED_FIELD = "paymentMethodsSyncInitialized"
    }
}
