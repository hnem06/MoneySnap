package com.devpro58.hnem06.moneysnap.domain.repository

import kotlinx.coroutines.flow.Flow

interface ConnectivityRepository {
    /** Emits `true` while the device has validated internet access. */
    fun observeIsOnline(): Flow<Boolean>
}
