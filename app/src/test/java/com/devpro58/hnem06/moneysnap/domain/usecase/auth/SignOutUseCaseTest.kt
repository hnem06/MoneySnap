package com.devpro58.hnem06.moneysnap.domain.usecase.auth

import com.devpro58.hnem06.moneysnap.domain.model.AuthUser
import com.devpro58.hnem06.moneysnap.domain.repository.AuthRepository
import com.devpro58.hnem06.moneysnap.domain.repository.ExpenseRepository
import com.devpro58.hnem06.moneysnap.domain.repository.PaymentMethodRepository
import com.devpro58.hnem06.moneysnap.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Sign-out used to call `firebaseAuth.signOut()` and nothing else, leaving the account's
 * expenses, receipt images and budget on the device for the next person to sign in.
 */
class SignOutUseCaseTest {

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val expenseRepository: ExpenseRepository = mockk(relaxed = true)
    private val paymentMethodRepository: PaymentMethodRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)

    private val signOut = SignOutUseCase(
        authRepository, expenseRepository, paymentMethodRepository, settingsRepository
    )

    private fun signedInAs(userId: String?) {
        every { authRepository.getCurrentUser() } returns userId?.let {
            AuthUser(id = it, email = "a@b.test", displayName = "A", photoUrl = null)
        }
    }

    @Test
    fun `wipes local account data before signing out`() = runTest {
        signedInAs(USER_ID)

        signOut()

        // Order matters: the user id comes from the live session, so clearing must happen while
        // the user is still signed in.
        coVerifyOrder {
            expenseRepository.clearLocalData(USER_ID)
            paymentMethodRepository.clearLocalData(USER_ID)
            settingsRepository.clearUserScopedSettings()
            authRepository.signOut()
        }
    }

    @Test
    fun `still signs out when clearing local data fails`() = runTest {
        signedInAs(USER_ID)
        coEvery { expenseRepository.clearLocalData(any()) } throws IllegalStateException("disk")

        signOut()

        // Being unable to delete a cached file must never strand the user in a signed-in session.
        coVerify { authRepository.signOut() }
    }

    @Test
    fun `clears device settings even with no active session`() = runTest {
        signedInAs(null)

        signOut()

        coVerify(exactly = 0) { expenseRepository.clearLocalData(any()) }
        coVerify { settingsRepository.clearUserScopedSettings() }
        coVerify { authRepository.signOut() }
    }

    private companion object {
        const val USER_ID = "user-1"
    }
}
