package com.devpro58.hnem06.moneysnap.presentation.security

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.core.utils.AuthExceptionHandler
import com.devpro58.hnem06.moneysnap.domain.usecase.auth.ChangePasswordUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.auth.DeleteAccountUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.auth.HasPasswordProviderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import timber.log.Timber

data class SecurityUiState(
    val busy: Boolean = false,
    @field:StringRes val message: Int? = null,
    /** Set once the account is gone and the screen must leave for the auth flow. */
    val accountDeleted: Boolean = false
)

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    hasPasswordProvider: HasPasswordProviderUseCase
) : ViewModel() {

    /** Google-only accounts have no password for Firebase to change. */
    val canChangePassword: Boolean = hasPasswordProvider()

    private val _uiState = MutableLiveData(SecurityUiState())
    val uiState: LiveData<SecurityUiState> = _uiState

    fun changePassword(currentPassword: String, newPassword: String) {
        if (_uiState.value?.busy == true) return
        _uiState.value = SecurityUiState(busy = true)

        viewModelScope.launch {
            _uiState.value = runCatching {
                changePasswordUseCase(currentPassword, newPassword)
            }.fold(
                onSuccess = { SecurityUiState(message = R.string.security_password_changed) },
                onFailure = { error ->
                    Timber.w(error, "changePassword failed")
                    // Reuses the existing auth error mapping so a wrong current password reads
                    // as "password is incorrect" rather than a generic failure.
                    SecurityUiState(message = AuthExceptionHandler.messageRes(error))
                }
            )
        }
    }

    fun deleteAccount(password: String?) {
        if (_uiState.value?.busy == true) return
        _uiState.value = SecurityUiState(busy = true)

        viewModelScope.launch {
            _uiState.value = runCatching { deleteAccountUseCase(password) }.fold(
                onSuccess = {
                    SecurityUiState(
                        message = R.string.security_deleted,
                        accountDeleted = true
                    )
                },
                onFailure = { error ->
                    Timber.e(error, "deleteAccount failed")
                    SecurityUiState(message = AuthExceptionHandler.messageRes(error))
                }
            )
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value?.copy(message = null)
    }
}
