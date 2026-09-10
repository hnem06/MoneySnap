package com.devpro58.hnem06.moneysnap.presentation.profile

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.domain.model.AuthUser
import com.devpro58.hnem06.moneysnap.domain.model.PaymentMethod
import com.devpro58.hnem06.moneysnap.domain.model.PaymentMethodSyncError
import com.devpro58.hnem06.moneysnap.domain.model.PaymentMethodSyncException
import com.devpro58.hnem06.moneysnap.domain.usecase.auth.GetCurrentUserUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.auth.RemoveAvatarUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.auth.SignOutUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.auth.UpdateAvatarUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.auth.UpdateDisplayNameUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.payment.AddPaymentMethodUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.payment.DeletePaymentMethodUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.payment.ObservePaymentMethodsUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.payment.UpdatePaymentMethodUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/** Account header state: user info + in-flight profile action + one-shot message. */
data class ProfileAccountState(
    val user: AuthUser? = null,
    val busy: Boolean = false,
    @field:StringRes val message: Int? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    observePaymentMethods: ObservePaymentMethodsUseCase,
    getCurrentUser: GetCurrentUserUseCase,
    private val addPaymentMethodUseCase: AddPaymentMethodUseCase,
    private val updatePaymentMethodUseCase: UpdatePaymentMethodUseCase,
    private val deletePaymentMethodUseCase: DeletePaymentMethodUseCase,
    private val updateDisplayNameUseCase: UpdateDisplayNameUseCase,
    private val updateAvatarUseCase: UpdateAvatarUseCase,
    private val removeAvatarUseCase: RemoveAvatarUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    val paymentMethods: LiveData<List<PaymentMethod>> =
        observePaymentMethods().asLiveData()

    private val _accountState = MutableLiveData(ProfileAccountState(user = getCurrentUser()))
    val accountState: LiveData<ProfileAccountState> = _accountState

    private val _signedOut = MutableLiveData<Boolean>()

    /** Emits once the account has been signed out and its local data wiped. */
    val signedOut: LiveData<Boolean> = _signedOut

    /**
     * Sign-out now clears Room, cached receipts and account settings, so it is suspending and
     * the screen must wait for it. Previously the fragment called `authRepository.signOut()`
     * directly and navigated away immediately, which both bypassed this ViewModel and left the
     * previous account's data on the device.
     */
    fun signOut() {
        viewModelScope.launch {
            runCatching { signOutUseCase() }
            // Navigate regardless: the session is gone either way, and stranding the user on a
            // half-signed-out Profile screen would be worse than a stale cache.
            _signedOut.value = true
        }
    }

    fun changeAvatar(localImageUri: String) = runAccountAction(R.string.avatar_updated) {
        updateAvatarUseCase(localImageUri)
    }

    fun deleteAvatar() = runAccountAction(R.string.avatar_removed) {
        removeAvatarUseCase()
    }

    fun changeDisplayName(name: String) = runAccountAction(R.string.personal_info_saved) {
        updateDisplayNameUseCase(name)
    }

    /** Marks the one-shot account message as shown. */
    fun consumeAccountMessage() {
        _accountState.value = _accountState.value?.copy(message = null)
    }

    private fun runAccountAction(
        @StringRes successMessage: Int,
        action: suspend () -> AuthUser
    ) {
        val current = _accountState.value ?: ProfileAccountState()
        if (current.busy) return
        _accountState.value = current.copy(busy = true, message = null)

        viewModelScope.launch {
            _accountState.value = runCatching { action() }.fold(
                onSuccess = { user ->
                    ProfileAccountState(user = user, busy = false, message = successMessage)
                },
                onFailure = {
                    current.copy(busy = false, message = R.string.profile_action_failed)
                }
            )
        }
    }

    private val _paymentMethodAction = MutableLiveData<PaymentMethodAction?>()
    val paymentMethodAction: LiveData<PaymentMethodAction?> = _paymentMethodAction

    fun addPaymentMethod(name: String) {
        viewModelScope.launch {
            _paymentMethodAction.value = runCatching {
                addPaymentMethodUseCase(name)
            }.fold(
                onSuccess = { PaymentMethodAction.Saved },
                onFailure = { it.toPaymentMethodAction() }
            )
        }
    }

    fun updatePaymentMethod(method: PaymentMethod, name: String) {
        if (method.isBuiltIn) {
            _paymentMethodAction.value = PaymentMethodAction.DefaultLocked
            return
        }

        viewModelScope.launch {
            _paymentMethodAction.value = runCatching {
                updatePaymentMethodUseCase(method.id, name)
            }.fold(
                onSuccess = { PaymentMethodAction.Saved },
                onFailure = { it.toPaymentMethodAction() }
            )
        }
    }

    fun deletePaymentMethod(method: PaymentMethod) {
        if (method.isBuiltIn) {
            _paymentMethodAction.value = PaymentMethodAction.DefaultLocked
            return
        }

        viewModelScope.launch {
            _paymentMethodAction.value = runCatching {
                deletePaymentMethodUseCase(method.id)
            }.fold(
                onSuccess = { PaymentMethodAction.Deleted },
                onFailure = { it.toPaymentMethodAction() }
            )
        }
    }

    fun consumePaymentMethodAction() {
        _paymentMethodAction.value = null
    }

    private fun Throwable.toPaymentMethodAction(): PaymentMethodAction =
        when ((this as? PaymentMethodSyncException)?.error) {
            PaymentMethodSyncError.PermissionDenied -> PaymentMethodAction.PermissionDenied
            PaymentMethodSyncError.Network -> PaymentMethodAction.NetworkError
            else -> PaymentMethodAction.Error
        }
}

enum class PaymentMethodAction {
    Saved,
    Deleted,
    DefaultLocked,
    PermissionDenied,
    NetworkError,
    Error
}
