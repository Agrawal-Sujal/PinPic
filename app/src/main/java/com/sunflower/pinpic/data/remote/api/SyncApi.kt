package com.sunflower.pinpic.data.remote.api

import com.sunflower.pinpic.data.remote.dto.DeltaSyncPullResponse
import com.sunflower.pinpic.data.remote.dto.SyncPushRequest
import com.sunflower.pinpic.data.remote.dto.SyncPushResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface SyncApi {
    @POST("sync/push")
    suspend fun pushMutations(
        @Header("Authorization") bearerToken: String,
        @Body request: SyncPushRequest
    ): Response<SyncPushResponse>

    @GET("sync/pull")
    suspend fun pullDeltas(
        @Header("Authorization") bearerToken: String,
        @Query("cursor") cursor: Long,
        @Query("limit") limit: Int,
        @Query("deviceId") deviceId: String? = null
    ): Response<DeltaSyncPullResponse>
}

