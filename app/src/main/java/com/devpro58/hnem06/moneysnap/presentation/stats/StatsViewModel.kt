package com.devpro58.hnem06.moneysnap.presentation.stats

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.devpro58.hnem06.moneysnap.domain.model.HistoryFilter
import com.devpro58.hnem06.moneysnap.domain.model.TransactionType
import com.devpro58.hnem06.moneysnap.domain.usecase.expense.GetExpenseHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import timber.log.Timber

@HiltViewModel
class StatsViewModel @Inject constructor(
    getExpenseHistory: GetExpenseHistoryUseCase
) : ViewModel() {

    /**
     * Spending only. Every chart on this screen answers "where did my money go", so income would
     * distort the monthly total, the daily line, the weekly bars and the category donut alike —
     * filtering at the source is safer than remembering to exclude it at four call sites.
     */
    val uiState: LiveData<StatsUiState> =
        getExpenseHistory(HistoryFilter(type = TransactionType.Expense))
            .map { StatsUiState.Content(it) as StatsUiState }
            .onStart { emit(StatsUiState.Loading) }
            // Previously `.catch { emit(emptyList()) }`, which rendered a database failure as
            // "you have no expenses" — indistinguishable from a genuinely empty month.
            .catch { error ->
                Timber.e(error, "Stats load failed")
                emit(StatsUiState.Error(error.localizedMessage.orEmpty()))
            }
            .asLiveData()
}
