package com.devpro58.hnem06.moneysnap.domain.usecase.receipt

import com.devpro58.hnem06.moneysnap.domain.repository.ReceiptRecognitionRepository
import javax.inject.Inject

class ScanReceiptUseCase @Inject constructor(
    private val repository: ReceiptRecognitionRepository
) {
    suspend operator fun invoke(imageUri: String) = repository.recognize(imageUri)
}
