package com.devpro58.hnem06.moneysnap.domain.model

data class ReceiptDraft(
    val totalAmount: Long?,
    val spentAtMillis: Long?,
    val suggestedCategory: ExpenseCategory?,
    val rawText: String,
    val warnings: List<ReceiptParseWarning> = emptyList()
)

enum class ReceiptParseWarning {
    TotalNotFound,
    DateNotFound
}
