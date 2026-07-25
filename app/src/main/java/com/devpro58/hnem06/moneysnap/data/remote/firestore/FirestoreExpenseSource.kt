package com.devpro58.hnem06.moneysnap.data.remote.firestore

import com.devpro58.hnem06.moneysnap.data.local.entity.ExpenseEntity
import com.devpro58.hnem06.moneysnap.data.remote.firebase.awaitTask
import com.devpro58.hnem06.moneysnap.domain.model.ExpenseSyncStatus
import com.devpro58.hnem06.moneysnap.domain.model.ReceiptUploadStatus
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
     * Realtime stream of every expense stored remotely for [userId].
     * Emits the full list on each snapshot; the listener is removed when the flow closes.
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
                if (snapshot == null) return@addSnapshotListener
                trySend(snapshot.documents.mapNotNull { it.toExpenseEntity() })
            }
        awaitClose { registration.remove() }
    }

    /** Maps a remote document back to a local entity. [localReceiptPath] stays null — the
     *  receipt image lives on the device that captured it; other devices use [remoteReceiptUrl]. */
    private fun DocumentSnapshot.toExpenseEntity(): ExpenseEntity? {
        val userId = getString("userId") ?: return null
        val amount = getLong("amount") ?: return null
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
            updatedAtMillis = getLong("updatedAtMillis") ?: 0L
        )
    }

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
            "syncStatus" to expense.syncStatus,
            "spentAtMillis" to expense.spentAtMillis,
            "createdAtMillis" to expense.createdAtMillis,
            "updatedAtMillis" to expense.updatedAtMillis
        )

        firestore.collection("users")
            .document(expense.userId)
            .collection("expenses")
            .document(expense.id)
            .set(data)
            .awaitTask()
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
