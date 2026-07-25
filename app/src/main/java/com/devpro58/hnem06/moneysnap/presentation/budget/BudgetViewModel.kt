package com.devpro58.hnem06.moneysnap.presentation.budget

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.devpro58.hnem06.moneysnap.domain.usecase.settings.GetMonthlyBudgetUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.settings.SetMonthlyBudgetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    getMonthlyBudget: GetMonthlyBudgetUseCase,
    private val setMonthlyBudget: SetMonthlyBudgetUseCase
) : ViewModel() {

    private val _budget = MutableLiveData(getMonthlyBudget())
    val budget: LiveData<Long> = _budget

    private val _saved = MutableLiveData(false)
    val saved: LiveData<Boolean> = _saved

    fun updateBudget(amount: Long) {
        _budget.value = amount.coerceIn(MIN_BUDGET, MAX_BUDGET)
    }

    fun save() {
        val amount = _budget.value ?: DEFAULT_BUDGET
        setMonthlyBudget(amount)
        _saved.value = true
    }

    fun consumeSaved() {
        _saved.value = false
    }

    companion object {
        const val STEP = 1_000_000L
        const val MIN_BUDGET = 1_000_000L
        const val MAX_BUDGET = 50_000_000L
        const val DEFAULT_BUDGET = 5_000_000L
    }
}
