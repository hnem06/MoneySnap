package com.devpro58.hnem06.moneysnap.domain.usecase.network

import com.devpro58.hnem06.moneysnap.domain.repository.ConnectivityRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveConnectivityUseCase @Inject constructor(
    private val connectivityRepository: ConnectivityRepository
) {
    operator fun invoke(): Flow<Boolean> = connectivityRepository.observeIsOnline()
}
