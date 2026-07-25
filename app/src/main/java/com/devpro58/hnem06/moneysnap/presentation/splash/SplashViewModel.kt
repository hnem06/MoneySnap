package com.devpro58.hnem06.moneysnap.presentation.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devpro58.hnem06.moneysnap.domain.model.AuthSessionStatus
import com.devpro58.hnem06.moneysnap.domain.usecase.auth.CheckAuthSessionUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.onboarding.IsOnboardingCompletedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val checkAuthSession: CheckAuthSessionUseCase,
    private val isOnboardingCompleted: IsOnboardingCompletedUseCase
) : ViewModel() {

    private val _destination = MutableLiveData<SplashDestination>()
    val destination: LiveData<SplashDestination> = _destination

    fun resolveDestination() {
        viewModelScope.launch {
            _destination.value = when (checkAuthSession()) {
                AuthSessionStatus.Authenticated -> SplashDestination.Main()
                AuthSessionStatus.InvalidSession -> SplashDestination.Main(showSessionError = true)
                AuthSessionStatus.OfflineAllowed -> SplashDestination.Main()
                AuthSessionStatus.Unauthenticated -> {
                    if (isOnboardingCompleted()) SplashDestination.Auth else SplashDestination.Welcome
                }
            }
        }
    }
}
