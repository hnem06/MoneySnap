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
import org.junit.Assert.assertThrows
import org.junit.Test

class DeleteAccountUseCaseTest {

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val expenseRepository: ExpenseRepository = mockk(relaxed = true)
    private val paymentMethodRepository: PaymentMethodRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)

    private val deleteAccount = DeleteAccountUseCase(
        authRepository, expenseRepository, paymentMethodRepository, settingsRepository
    )

    init {
        every { authRepository.getCurrentUser() } returns
            AuthUser(id = USER_ID, email = "a@b.test", displayName = "A", photoUrl = null)
    }

    @Test
    fun `deletes remotely before clearing local caches`() = runTest {
        deleteAccount(PASSWORD)

        // Local data is the user's only copy until the remote delete has actually succeeded.
        coVerifyOrder {
            authRepository.deleteAccount(PASSWORD)
            expenseRepository.clearLocalData(USER_ID)
            paymentMethodRepository.clearLocalData(USER_ID)
            settingsRepository.clearUserScopedSettings()
        }
    }

    @Test
    fun `keeps local data when the remote delete fails`() = runTest {
        coEvery { authRepository.deleteAccount(any()) } throws IllegalStateException("network")

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { deleteAccount(PASSWORD) }
        }

        // Wiping Room here would leave a still-existing account with an empty app.
        coVerify(exactly = 0) { expenseRepository.clearLocalData(any()) }
        coVerify(exactly = 0) { settingsRepository.clearUserScopedSettings() }
    }

    @Test
    fun `passes a null password through for accounts without one`() = runTest {
        deleteAccount(null)

        // Google-only accounts cannot be re-authenticated with a password; the repository
        // decides, not the caller.
        coVerify { authRepository.deleteAccount(null) }
    }

    private companion object {
        const val USER_ID = "user-1"
        const val PASSWORD = "hunter2"
    }
}
