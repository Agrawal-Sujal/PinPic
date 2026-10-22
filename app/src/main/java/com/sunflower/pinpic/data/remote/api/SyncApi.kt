package com.sunflower.pinpic.data.remote.api

import com.sunflower.pinpic.data.remote.dto.SyncBatchRequest
import com.sunflower.pinpic.data.remote.dto.SyncBatchResponse
import com.sunflower.pinpic.data.remote.dto.SyncPullResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface SyncApi {
    @POST("sync/batch")
    suspend fun syncBatch(
        @Header("Authorization") bearerToken: String,
        @Body request: SyncBatchRequest
    ): Response<SyncBatchResponse>

    @GET("sync/pull")
    suspend fun pullChanges(
        @Header("Authorization") bearerToken: String,
        @Query("lastOperationId") lastOperationId: Long,
        @Query("batchSize") batchSize: Int,
        @Query("deviceId") deviceId: String? = null
    ): Response<SyncPullResponse>
}

