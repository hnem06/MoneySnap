package com.devpro58.hnem06.moneysnap.data.local.file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptImageLocalDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    fun receiptFile(userId: String, expenseId: String): File =
        File(File(context.filesDir, "receipts/$userId"), "$expenseId.jpg")

    fun copyReceipt(userId: String, expenseId: String, sourceUriString: String?): String? {
        if (sourceUriString.isNullOrBlank()) return null

        val target = receiptFile(userId, expenseId)
        target.parentFile?.mkdirs()

        val sourceUri = Uri.parse(sourceUriString)
        val bitmap = decodeSampledBitmap(sourceUri) ?: return null

        target.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, output)
        }
        bitmap.recycle()

        return target.absolutePath
    }

    fun deleteReceipt(localPath: String?) {
        if (localPath.isNullOrBlank()) return
        File(localPath).delete()
    }

    /**
     * Removes every cached receipt image for a user. Called on sign-out: the images are
     * re-downloadable from Storage, and leaving one account's receipts readable on a shared
     * device after the next person signs in is a privacy leak.
     */
    fun deleteAllForUser(userId: String) {
        File(context.filesDir, "receipts/$userId").deleteRecursively()
    }

    /**
     * Decode bitmap from Uri, downsampling if the image exceeds [MAX_DIMENSION].
     * This prevents storing raw camera photos (typically 3-8 MB) at full resolution.
     */
    private fun decodeSampledBitmap(uri: Uri): Bitmap? {
        // First pass: read dimensions only
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }

        val width = options.outWidth
        val height = options.outHeight
        if (width <= 0 || height <= 0) return null

        // Calculate inSampleSize
        var sampleSize = 1
        if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
            val halfWidth = width / 2
            val halfHeight = height / 2
            while (halfWidth / sampleSize >= MAX_DIMENSION && halfHeight / sampleSize >= MAX_DIMENSION) {
                sampleSize *= 2
            }
        }

        // Second pass: decode with sample size
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        }
    }

    private companion object {
        const val MAX_DIMENSION = 1280
        const val COMPRESS_QUALITY = 85
    }
}

