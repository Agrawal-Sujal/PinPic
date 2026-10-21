package com.sunflower.pinpic.data.repository

import android.os.Build
import com.sunflower.pinpic.common.Result
import com.sunflower.pinpic.data.local.datastore.DataStoreManager
import com.sunflower.pinpic.data.remote.api.DeviceApi
import com.sunflower.pinpic.data.remote.dto.DeviceRegistrationRequest
import com.sunflower.pinpic.data.remote.parseResponse
import com.sunflower.pinpic.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val deviceApi: DeviceApi,
    private val dataStoreManager: DataStoreManager
) : DeviceRepository {

    override val deviceIdFlow: Flow<String?> = dataStoreManager.deviceIdFlow

    override suspend fun getOrRegisterDeviceId(jwtToken: String?, userId: String?): String? {
        val currentDeviceId = dataStoreManager.deviceIdFlow.firstOrNull()
        if (!currentDeviceId.isNullOrEmpty()) {
            return currentDeviceId
        }

        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
        val bearerToken = jwtToken?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" }

        val result = parseResponse {
            deviceApi.registerDevice(
                bearerToken = bearerToken,
                request = DeviceRegistrationRequest(
                    device = deviceName,
                    userId = userId
                )
            )
        }

        return when (result) {
            is Result.Success -> {
                val newDeviceId = result.data.deviceId
                if (!newDeviceId.isNullOrEmpty()) {
                    dataStoreManager.saveDeviceId(newDeviceId)
                }
                newDeviceId
            }
            is Result.Error -> null
        }
    }
}
