package com.sunflower.pinpic.domain.repository

import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    val deviceIdFlow: Flow<String?>
    suspend fun getOrRegisterDeviceId(jwtToken: String? = null, userId: String? = null): String?
}
