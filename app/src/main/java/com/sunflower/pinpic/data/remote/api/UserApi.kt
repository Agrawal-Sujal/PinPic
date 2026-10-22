package com.sunflower.pinpic.data.remote.api

import com.sunflower.pinpic.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface UserApi {
    @GET("users/profile")
    suspend fun getProfile(
        @Header("Authorization") bearerToken: String
    ): Response<UserProfileResponse>

    @POST("profile-photo/initiate-upload")
    suspend fun initiateUpload(
        @Header("Authorization") bearerToken: String,
        @Body request: InitiateUploadRequest
    ): Response<InitiateUploadResponse>

    @POST("profile-photo/confirm-upload")
    suspend fun confirmUpload(
        @Header("Authorization") bearerToken: String,
        @Body request: ConfirmUploadRequest
    ): Response<ConfirmUploadResponse>

    @POST("users/profile")
    suspend fun updateProfile(
        @Header("Authorization") bearerToken: String,
        @Body request: UpdateProfileRequest
    ): Response<Unit>
}
