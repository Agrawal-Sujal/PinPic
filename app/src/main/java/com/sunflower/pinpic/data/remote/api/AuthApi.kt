package com.sunflower.pinpic.data.remote.api

import com.sunflower.pinpic.data.remote.dto.AuthResponse
import com.sunflower.pinpic.data.remote.dto.GoogleAuthRequest
import com.sunflower.pinpic.data.remote.dto.LoginRequest
import com.sunflower.pinpic.data.remote.dto.RegisterRequest
import com.sunflower.pinpic.data.remote.dto.SetUsernameRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<Unit>

    @POST("auth/google")
    suspend fun googleAuth(@Body request: GoogleAuthRequest): Response<AuthResponse>

    @PATCH("users/username")
    suspend fun setUsername(
        @Header("Authorization") bearerToken: String,
        @Body request: SetUsernameRequest
    ): Response<Unit>
}
