package com.devpro58.hnem06.moneysnap.presentation.history

import com.devpro58.hnem06.moneysnap.domain.model.Expense

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data class Content(val expenses: List<Expense>) : HistoryUiState
    data class Error(val message: String) : HistoryUiState
}
