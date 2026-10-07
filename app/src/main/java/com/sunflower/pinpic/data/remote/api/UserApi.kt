package com.sunflower.pinpic.data.remote.api

import com.sunflower.pinpic.data.remote.dto.UserProfileResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface UserApi {
    @GET("users/profile")
    suspend fun getProfile(
        @Header("Authorization") bearerToken: String
    ): Response<UserProfileResponse>
}
