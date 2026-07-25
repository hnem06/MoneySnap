package com.devpro58.hnem06.moneysnap.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devpro58.hnem06.moneysnap.domain.usecase.auth.SignOutUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.expense.SyncExpensesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val signOut: SignOutUseCase,
    private val syncExpenses: SyncExpensesUseCase
) : ViewModel() {

    init {
        // Realtime down-sync while the main screen is alive; stops when the ViewModel clears.
        viewModelScope.launch {
            runCatching { syncExpenses() }
        }
    }

    fun clearInvalidSession() {
        signOut()
    }
}
