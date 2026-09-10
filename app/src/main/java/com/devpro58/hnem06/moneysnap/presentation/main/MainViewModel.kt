package com.devpro58.hnem06.moneysnap.presentation.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.devpro58.hnem06.moneysnap.domain.usecase.auth.SignOutUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.expense.SyncExpensesUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.network.ObserveConnectivityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val signOut: SignOutUseCase,
    private val syncExpenses: SyncExpensesUseCase,
    observeConnectivity: ObserveConnectivityUseCase
) : ViewModel() {

    /** Drives the offline banner. Starts optimistic so the banner never flashes on a cold start. */
    val isOnline: LiveData<Boolean> = observeConnectivity().asLiveData()

    init {
        // Realtime down-sync while the main screen is alive; stops when the ViewModel clears.
        viewModelScope.launch {
            runCatching { syncExpenses() }
        }
    }

    fun clearInvalidSession() {
        viewModelScope.launch {
            runCatching { signOut() }
        }
    }
}
