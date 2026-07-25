package com.devpro58.hnem06.moneysnap.presentation.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devpro58.hnem06.moneysnap.domain.usecase.auth.SignInWithEmailUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.auth.SignInWithGoogleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signInWithEmail: SignInWithEmailUseCase,
    private val signInWithGoogle: SignInWithGoogleUseCase
) : ViewModel() {

    private val _uiState = MutableLiveData<AuthUiState>(AuthUiState.Idle)
    val uiState: LiveData<AuthUiState> = _uiState

    fun loginWithEmail(email: String, password: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            runCatching { signInWithEmail(email, password) }
                .onSuccess { _uiState.value = AuthUiState.Authenticated }
                .onFailure { _uiState.value = AuthUiState.Error(it) }
        }
    }

    fun loginWithGoogle(idToken: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            runCatching { signInWithGoogle(idToken) }
                .onSuccess { _uiState.value = AuthUiState.Authenticated }
                .onFailure { _uiState.value = AuthUiState.Error(it) }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
