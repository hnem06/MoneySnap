package com.devpro58.hnem06.moneysnap.data.repository

import com.devpro58.hnem06.moneysnap.core.network.NetworkMonitor
import com.devpro58.hnem06.moneysnap.domain.repository.ConnectivityRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class ConnectivityRepositoryImpl @Inject constructor(
    private val networkMonitor: NetworkMonitor
) : ConnectivityRepository {

    override fun observeIsOnline(): Flow<Boolean> = networkMonitor.observeIsOnline()
}
