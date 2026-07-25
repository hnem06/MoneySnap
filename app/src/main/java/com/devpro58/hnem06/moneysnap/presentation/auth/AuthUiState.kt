package com.devpro58.hnem06.moneysnap.presentation.auth

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Authenticated : AuthUiState
    data object PasswordResetEmailSent : AuthUiState
    data class Error(val throwable: Throwable) : AuthUiState
}
