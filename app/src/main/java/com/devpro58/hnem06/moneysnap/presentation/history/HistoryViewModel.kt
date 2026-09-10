package com.devpro58.hnem06.moneysnap.presentation.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.devpro58.hnem06.moneysnap.domain.model.Expense
import com.devpro58.hnem06.moneysnap.domain.model.ExpenseCategory
import com.devpro58.hnem06.moneysnap.domain.model.HistoryFilter
import com.devpro58.hnem06.moneysnap.domain.usecase.expense.DeleteExpenseUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.expense.GetExpenseHistoryUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.expense.RetryReceiptUploadUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.expense.UpdateExpenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getExpenseHistory: GetExpenseHistoryUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase,
    private val retryReceiptUploadUseCase: RetryReceiptUploadUseCase
) : ViewModel() {

    private val filter = MutableLiveData(HistoryFilter())

    /** One-shot event signals: consumed once by observers */
    private val _deleteResult = MutableLiveData<Result<Unit>?>()
    val deleteResult: LiveData<Result<Unit>?> = _deleteResult

    private val _updateResult = MutableLiveData<Result<Unit>?>()
    val updateResult: LiveData<Result<Unit>?> = _updateResult

    val uiState: LiveData<HistoryUiState> =
        filter.switchMap { currentFilter ->
            getExpenseHistory(currentFilter)
                .map { expenses -> HistoryUiState.Content(expenses) as HistoryUiState }
                .catch { emit(HistoryUiState.Error(it.localizedMessage.orEmpty())) }
                .asLiveData()
        }

    fun updateSearch(query: String) {
        filter.value = filter.value?.copy(query = query) ?: HistoryFilter(query = query)
    }

    fun updateCategory(category: ExpenseCategory?) {
        filter.value = filter.value?.copy(category = category) ?: HistoryFilter(category = category)
    }

    fun updateDateRange(startMillis: Long?, endMillis: Long?) {
        filter.value = filter.value?.copy(
            dateRangeStart = startMillis,
            dateRangeEnd = endMillis
        ) ?: HistoryFilter(dateRangeStart = startMillis, dateRangeEnd = endMillis)
    }

    fun updateAmountRange(min: Long?, max: Long?) {
        filter.value = filter.value?.copy(
            amountMin = min,
            amountMax = max
        ) ?: HistoryFilter(amountMin = min, amountMax = max)
    }

    fun clearFilters() {
        filter.value = HistoryFilter(query = filter.value?.query.orEmpty())
    }

    /** Re-queues a failed upload. Fire-and-forget: the outcome shows up as a status change. */
    fun retrySync(expenseId: String) {
        viewModelScope.launch {
            runCatching { retryReceiptUploadUseCase(expenseId) }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            _deleteResult.value = runCatching { deleteExpenseUseCase(expenseId) }
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            _updateResult.value = runCatching { updateExpenseUseCase(expense) }
        }
    }

    /** Clear one-shot events after consumption */
    fun consumeDeleteResult() { _deleteResult.value = null }
    fun consumeUpdateResult() { _updateResult.value = null }
}
