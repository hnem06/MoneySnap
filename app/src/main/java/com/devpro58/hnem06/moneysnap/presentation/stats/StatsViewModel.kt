package com.devpro58.hnem06.moneysnap.presentation.stats

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.devpro58.hnem06.moneysnap.domain.model.Expense
import com.devpro58.hnem06.moneysnap.domain.model.HistoryFilter
import com.devpro58.hnem06.moneysnap.domain.usecase.expense.GetExpenseHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val getExpenseHistory: GetExpenseHistoryUseCase
) : ViewModel() {

    /** All expenses for the current user — stats are computed in the fragment. */
    val expenses: LiveData<List<Expense>> =
        getExpenseHistory(HistoryFilter())
            .catch { emit(emptyList()) }
            .asLiveData()
}
