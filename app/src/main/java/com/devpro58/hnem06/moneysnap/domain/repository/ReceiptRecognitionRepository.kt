package com.devpro58.hnem06.moneysnap.domain.repository

import com.devpro58.hnem06.moneysnap.domain.model.ReceiptDraft

interface ReceiptRecognitionRepository {
    suspend fun recognize(imageUri: String): ReceiptDraft
}
