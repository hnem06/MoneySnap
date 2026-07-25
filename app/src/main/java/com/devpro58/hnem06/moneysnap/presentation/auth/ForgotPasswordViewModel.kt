package com.devpro58.hnem06.moneysnap.presentation.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devpro58.hnem06.moneysnap.domain.usecase.auth.SendPasswordResetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val sendPasswordReset: SendPasswordResetUseCase
) : ViewModel() {

    private val _uiState = MutableLiveData<AuthUiState>(AuthUiState.Idle)
    val uiState: LiveData<AuthUiState> = _uiState

    fun sendResetEmail(email: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            runCatching { sendPasswordReset(email) }
                .onSuccess { _uiState.value = AuthUiState.PasswordResetEmailSent }
                .onFailure { _uiState.value = AuthUiState.Error(it) }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
