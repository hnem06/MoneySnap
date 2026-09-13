package com.devpro58.hnem06.moneysnap.presentation.stats

import com.devpro58.hnem06.moneysnap.domain.model.Expense

/**
 * There is deliberately no `Empty` case. Replacing the whole screen when a month has no data would
 * take the month selector away with it, trapping the user on an empty month with no way back —
 * emptiness is rendered per-card instead.
 */
sealed interface StatsUiState {
    data object Loading : StatsUiState
    data class Content(val expenses: List<Expense>) : StatsUiState
    data class Error(val message: String) : StatsUiState
}
