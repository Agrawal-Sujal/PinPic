package com.sunflower.pinpic.data.remote.api

import com.sunflower.pinpic.data.remote.dto.ConfirmUploadRequest
import com.sunflower.pinpic.data.remote.dto.ConfirmUploadResponse
import com.sunflower.pinpic.data.remote.dto.InitiateUploadRequest
import com.sunflower.pinpic.data.remote.dto.InitiateUploadResponse
import com.sunflower.pinpic.data.remote.dto.UpdateProfileRequest
import com.sunflower.pinpic.data.remote.dto.UserClusterResponse
import com.sunflower.pinpic.data.remote.dto.UserPhotoResponse
import com.sunflower.pinpic.data.remote.dto.UserProfileResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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

    @GET("users/search")
    suspend fun searchUsers(
        @Header("Authorization") bearerToken: String,
        @Query("query") query: String
    ): Response<List<UserProfileResponse>>

    @GET("users/{userId}/profile")
    suspend fun getUserProfile(
        @Header("Authorization") bearerToken: String,
        @Path("userId") userId: String
    ): Response<UserProfileResponse>

    @GET("users/{userId}/photos")
    suspend fun getUserPhotos(
        @Header("Authorization") bearerToken: String,
        @Path("userId") userId: String
    ): Response<List<UserPhotoResponse>>

    @GET("users/{userId}/clusters")
    suspend fun getUserClusters(
        @Header("Authorization") bearerToken: String,
        @Path("userId") userId: String
    ): Response<List<UserClusterResponse>>

    @POST("users/follow/{followingId}")
    suspend fun followUser(
        @Header("Authorization") bearerToken: String,
        @Path("followingId") followingId: String
    ): Response<Unit>

    @DELETE("users/follow/{followingId}")
    suspend fun unfollowUser(
        @Header("Authorization") bearerToken: String,
        @Path("followingId") followingId: String
    ): Response<Unit>
}
