package com.devpro58.hnem06.moneysnap.presentation.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devpro58.hnem06.moneysnap.domain.usecase.auth.RegisterWithEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerWithEmail: RegisterWithEmailUseCase
) : ViewModel() {

    private val _uiState = MutableLiveData<AuthUiState>(AuthUiState.Idle)
    val uiState: LiveData<AuthUiState> = _uiState

    fun register(name: String, email: String, password: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            runCatching { registerWithEmail(name, email, password) }
                .onSuccess { _uiState.value = AuthUiState.Authenticated }
                .onFailure { _uiState.value = AuthUiState.Error(it) }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
