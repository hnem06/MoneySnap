package com.devpro58.hnem06.moneysnap.data.receipt

import android.content.Context
import android.net.Uri
import com.devpro58.hnem06.moneysnap.data.remote.firebase.awaitTask
import com.devpro58.hnem06.moneysnap.domain.model.ReceiptDraft
import com.devpro58.hnem06.moneysnap.domain.repository.ReceiptRecognitionRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MlKitReceiptRecognitionRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ReceiptRecognitionRepository {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognize(imageUri: String): ReceiptDraft {
        val image = InputImage.fromFilePath(context, Uri.parse(imageUri))
        val result = recognizer.process(image).awaitTask()
        return VietnameseReceiptParser.parse(result.text)
    }
}
