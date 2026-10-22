package com.sunflower.pinpic.data.remote.api

import com.sunflower.pinpic.data.remote.dto.InitiateUploadRequest
import com.sunflower.pinpic.data.remote.dto.InitiateUploadResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface PhotoApi {
    @POST("photos/initiate-upload")
    suspend fun initiateUpload(
        @Header("Authorization") bearerToken: String,
        @Body request: InitiateUploadRequest
    ): Response<InitiateUploadResponse>
}

