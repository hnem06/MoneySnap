package com.devpro58.hnem06.moneysnap.presentation.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.devpro58.hnem06.moneysnap.domain.model.Expense
import com.devpro58.hnem06.moneysnap.domain.usecase.expense.DeleteExpenseUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.expense.GetHomeDashboardUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.expense.UpdateExpenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    getHomeDashboard: GetHomeDashboardUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase
) : ViewModel() {

    val uiState: LiveData<HomeUiState> =
        getHomeDashboard()
            .map { dashboard -> HomeUiState.Content(dashboard) as HomeUiState }
            .catch { emit(HomeUiState.Error(it.localizedMessage.orEmpty())) }
            .asLiveData()

    private val _actionResult = MutableLiveData<HomeExpenseActionResult?>()
    val actionResult: LiveData<HomeExpenseActionResult?> = _actionResult

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            _actionResult.value = HomeExpenseActionResult(
                action = HomeExpenseAction.Delete,
                result = runCatching { deleteExpenseUseCase(expenseId) }
            )
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            _actionResult.value = HomeExpenseActionResult(
                action = HomeExpenseAction.Update,
                result = runCatching { updateExpenseUseCase(expense) }
            )
        }
    }

    fun consumeActionResult() {
        _actionResult.value = null
    }
}

data class HomeExpenseActionResult(
    val action: HomeExpenseAction,
    val result: Result<Unit>
)

enum class HomeExpenseAction {
    Delete,
    Update
}
