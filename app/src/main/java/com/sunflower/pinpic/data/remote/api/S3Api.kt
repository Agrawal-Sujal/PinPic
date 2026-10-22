package com.sunflower.pinpic.data.remote.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Url

interface S3Api {
    @PUT
    suspend fun uploadToPresignedUrl(
        @Url uploadUrl: String,
        @Header("Content-Type") contentType: String,
        @Body imageBody: RequestBody
    ): Response<ResponseBody>
}