package com.devpro58.hnem06.moneysnap.presentation.main

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.devpro58.hnem06.moneysnap.domain.repository.ConnectivityRepository
import com.devpro58.hnem06.moneysnap.domain.usecase.auth.SignOutUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.expense.SyncExpensesUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.network.ObserveConnectivityUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private val connectivity = MutableStateFlow(true)
    private val connectivityRepository = object : ConnectivityRepository {
        override fun observeIsOnline(): Flow<Boolean> = connectivity
    }

    private val signOut: SignOutUseCase = mockk(relaxed = true)
    private val syncExpenses: SyncExpensesUseCase = mockk(relaxed = true)

    @Before
    fun setUp() {
        // Unconfined, not Standard: asLiveData() collects on Dispatchers.Main, and a queueing
        // dispatcher would leave the collection pending so the observer only ever sees the
        // initial value.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = MainViewModel(
        signOut = signOut,
        syncExpenses = syncExpenses,
        observeConnectivity = ObserveConnectivityUseCase(connectivityRepository)
    )

    @Test
    fun `exposes connectivity changes so the offline banner can react`() = runTest {
        val viewModel = viewModel()
        val seen = mutableListOf<Boolean>()
        viewModel.isOnline.observeForever { seen.add(it) }

        connectivity.value = false
        connectivity.value = true

        assertEquals(listOf(true, false, true), seen)
    }

    @Test
    fun `starts online so the banner never flashes on a cold start`() = runTest {
        val viewModel = viewModel()
        var first: Boolean? = null
        viewModel.isOnline.observeForever { if (first == null) first = it }

        assertEquals(true, first)
    }

    @Test
    fun `a failing down-sync does not take the screen down with it`() = runTest {
        // syncExpenses() collects a realtime Firestore flow; a permission or network error must
        // stay contained, because MainActivity has already been shown by the time it runs.
        coEvery { syncExpenses() } throws IllegalStateException("firestore unavailable")

        viewModel()
    }

    private companion object
}
