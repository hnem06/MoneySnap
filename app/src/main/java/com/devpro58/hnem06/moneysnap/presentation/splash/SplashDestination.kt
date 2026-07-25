package com.devpro58.hnem06.moneysnap.presentation.splash

sealed interface SplashDestination {
    data object Welcome : SplashDestination
    data object Auth : SplashDestination
    data class Main(val showSessionError: Boolean = false) : SplashDestination
}
