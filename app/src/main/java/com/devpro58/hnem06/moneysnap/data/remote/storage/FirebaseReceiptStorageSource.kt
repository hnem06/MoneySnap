package com.devpro58.hnem06.moneysnap.data.remote.storage

import android.net.Uri
import com.devpro58.hnem06.moneysnap.data.remote.firebase.awaitTask
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseReceiptStorageSource @Inject constructor(
    private val firebaseStorage: FirebaseStorage
) {

    suspend fun uploadReceipt(
        userId: String,
        expenseId: String,
        localReceiptPath: String
    ): String {
        val receiptFile = File(localReceiptPath)
        require(receiptFile.exists()) { "Receipt file does not exist: $localReceiptPath" }

        val reference = firebaseStorage.reference
            .child("receipts")
            .child(userId)
            .child("$expenseId.jpg")

        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()

        reference.putFile(Uri.fromFile(receiptFile), metadata).awaitTask()
        return reference.downloadUrl.awaitTask().toString()
    }

    /**
     * Removes the uploaded receipt image for an expense being deleted. Without this every
     * deleted expense left its image in the bucket forever, billed and unreachable.
     *
     * A missing object is treated as success — the expense may never have had a receipt, or
     * another device may have deleted it already. Mirrors `FirebaseAuthRepository.removeAvatar`.
     */
    suspend fun deleteReceipt(userId: String, expenseId: String) {
        try {
            firebaseStorage.reference
                .child("receipts")
                .child(userId)
                .child("$expenseId.jpg")
                .delete()
                .awaitTask()
        } catch (exception: StorageException) {
            if (exception.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) throw exception
        }
    }

    /**
     * Deletes every receipt image belonging to a user, for account deletion.
     *
     * Note this needs `list` permission on the `receipts/{uid}` **prefix**, not just on the
     * objects under it — see the matching rule in storage.rules. Without that rule listAll()
     * fails and the cascade silently leaves every image behind.
     */
    suspend fun deleteAllReceiptsForUser(userId: String) {
        val folder = firebaseStorage.reference.child("receipts").child(userId)
        val listing = try {
            folder.listAll().awaitTask()
        } catch (exception: StorageException) {
            // Nothing was ever uploaded: an empty prefix does not exist as an object.
            if (exception.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) return
            throw exception
        }
        listing.items.forEach { item ->
            try {
                item.delete().awaitTask()
            } catch (exception: StorageException) {
                if (exception.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) throw exception
            }
        }
    }
}
