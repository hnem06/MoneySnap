package com.devpro58.hnem06.moneysnap.data.remote.firestore

import com.devpro58.hnem06.moneysnap.data.local.entity.ExpenseEntity
import com.devpro58.hnem06.moneysnap.data.remote.firebase.awaitTask
import com.devpro58.hnem06.moneysnap.domain.model.ExpenseSyncStatus
import com.devpro58.hnem06.moneysnap.domain.model.ReceiptUploadStatus
import com.devpro58.hnem06.moneysnap.domain.model.TransactionType
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Singleton
class FirestoreExpenseSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    /**
     * Realtime stream of every expense stored **server-side** for [userId].
     * Emits the full list on each snapshot; the listener is removed when the flow closes.
     *
     * Cache-only snapshots are dropped on purpose. Firestore's offline persistence replays the
     * local cache before the server responds, and that cache is empty after an app-data clear or
     * a fresh install even when Room still holds the user's expenses. Feeding such a snapshot to
     * `reconcile()` would make it delete every local row that has no pending local change — i.e.
     * silently wipe the user's history. Offline, Room is the source of truth and no reconcile is
     * needed; the listener re-emits from the server as soon as connectivity returns.
     */
    fun observeExpenses(userId: String): Flow<List<ExpenseEntity>> = callbackFlow {
        val registration = firestore.collection("users")
            .document(userId)
            .collection("expenses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot == null || snapshot.metadata.isFromCache) return@addSnapshotListener
                trySend(snapshot.documents.mapNotNull { it.toExpenseEntity() })
            }
        awaitClose { registration.remove() }
    }

    /**
     * Maps a remote document back to a local entity. [localReceiptPath] stays null — the receipt
     * image lives on the device that captured it; other devices use [remoteReceiptUrl].
     *
     * Documents whose `schemaVersion` exceeds what this build understands are skipped rather than
     * misread. Without that guard, an older build reading an income document would fall back to
     * `type = Expense` and silently count a salary as spending: inflated totals, a wrong balance
     * and a false over-budget alert. Skipping loses nothing — the row reappears once that device
     * updates.
     */
    private fun DocumentSnapshot.toExpenseEntity(): ExpenseEntity? {
        val userId = getString("userId") ?: return null
        val amount = getLong("amount") ?: return null
        val schemaVersion = getLong("schemaVersion") ?: LEGACY_SCHEMA_VERSION
        if (schemaVersion > CURRENT_SCHEMA_VERSION) return null

        return ExpenseEntity(
            id = getString("id") ?: id,
            userId = userId,
            amount = amount,
            currency = getString("currency").orEmpty(),
            title = getString("title").orEmpty(),
            category = getString("category").orEmpty(),
            paymentMethod = getString("paymentMethod"),
            note = getString("note"),
            localReceiptPath = null,
            remoteReceiptUrl = getString("remoteReceiptUrl"),
            receiptUploadStatus = getString("receiptUploadStatus")
                ?: ReceiptUploadStatus.None.name,
            syncStatus = ExpenseSyncStatus.Synced.name,
            spentAtMillis = getLong("spentAtMillis") ?: 0L,
            createdAtMillis = getLong("createdAtMillis") ?: 0L,
            updatedAtMillis = getLong("updatedAtMillis") ?: 0L,
            // Absent on every document written before income existed. Defaulting to Expense is
            // not a fallback but the correct reading: the app only tracked spending then.
            type = getString("type") ?: TransactionType.Expense.name
        )
    }

    /**
     * Writes the expense to `users/{uid}/expenses/{id}`.
     *
     * [ExpenseEntity.syncStatus] is intentionally **not** part of the payload: it describes this
     * device's upload progress, not the expense. It was write-only noise anyway — [toExpenseEntity]
     * already hardcodes `Synced` when reading a document back. Keep the key set in sync with the
     * Firestore rules validation.
     */
    suspend fun upsertExpense(expense: ExpenseEntity) {
        val data = mapOf(
            "id" to expense.id,
            "userId" to expense.userId,
            "amount" to expense.amount,
            "currency" to expense.currency,
            "title" to expense.title,
            "category" to expense.category,
            "paymentMethod" to expense.paymentMethod,
            "note" to expense.note,
            "remoteReceiptUrl" to expense.remoteReceiptUrl,
            "receiptUploadStatus" to expense.receiptUploadStatus,
            "spentAtMillis" to expense.spentAtMillis,
            "createdAtMillis" to expense.createdAtMillis,
            "updatedAtMillis" to expense.updatedAtMillis,
            "type" to expense.type,
            "schemaVersion" to CURRENT_SCHEMA_VERSION
        )

        firestore.collection("users")
            .document(expense.userId)
            .collection("expenses")
            .document(expense.id)
            .set(data)
            .awaitTask()
    }

    private companion object {
        /**
         * Bumped whenever a field is added that an older build would misread. Documents written
         * before this field existed are treated as version 1.
         */
        const val CURRENT_SCHEMA_VERSION = 2L
        const val LEGACY_SCHEMA_VERSION = 1L
    }

    suspend fun deleteExpense(userId: String, expenseId: String) {
        firestore.collection("users")
            .document(userId)
            .collection("expenses")
            .document(expenseId)
            .delete()
            .awaitTask()
    }
}
