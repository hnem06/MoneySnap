package com.devpro58.hnem06.moneysnap.presentation.expense.add

sealed interface AddExpenseUiState {
    data object Idle : AddExpenseUiState
    data object Saving : AddExpenseUiState
    data object Saved : AddExpenseUiState
    data class Error(val message: String) : AddExpenseUiState
}

sealed interface ReceiptScanUiState {
    data object Idle : ReceiptScanUiState
    data object Scanning : ReceiptScanUiState
    data class Recognized(val draft: com.devpro58.hnem06.moneysnap.domain.model.ReceiptDraft) : ReceiptScanUiState
    data object NoUsefulData : ReceiptScanUiState
    data object Error : ReceiptScanUiState
}
