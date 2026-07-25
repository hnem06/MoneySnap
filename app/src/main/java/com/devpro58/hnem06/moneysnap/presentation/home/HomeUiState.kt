package com.devpro58.hnem06.moneysnap.presentation.home

import com.devpro58.hnem06.moneysnap.domain.model.HomeDashboard

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Content(val dashboard: HomeDashboard) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
