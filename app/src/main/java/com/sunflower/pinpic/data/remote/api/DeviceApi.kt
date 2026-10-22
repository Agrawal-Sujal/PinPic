package com.sunflower.pinpic.data.remote.api

import com.sunflower.pinpic.data.remote.dto.DeviceRegistrationRequest
import com.sunflower.pinpic.data.remote.dto.DeviceResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface DeviceApi {
    @POST("devices")
    suspend fun registerDevice(
        @Header("Authorization") bearerToken: String? = null,
        @Body request: DeviceRegistrationRequest
    ): Response<DeviceResponse>
}
