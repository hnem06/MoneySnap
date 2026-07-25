package com.devpro58.hnem06.moneysnap.data.remote.storage

import android.net.Uri
import com.devpro58.hnem06.moneysnap.data.remote.firebase.awaitTask
import com.google.firebase.storage.FirebaseStorage
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
}
