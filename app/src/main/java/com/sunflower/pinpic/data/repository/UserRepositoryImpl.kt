package com.sunflower.pinpic.data.repository

import android.net.Uri
import com.sunflower.pinpic.common.Result
import com.sunflower.pinpic.data.local.datastore.DataStoreManager
import com.sunflower.pinpic.data.remote.ImageUploader
import com.sunflower.pinpic.data.remote.api.UserApi
import com.sunflower.pinpic.data.remote.dto.ConfirmUploadRequest
import com.sunflower.pinpic.data.remote.dto.InitiateUploadRequest
import com.sunflower.pinpic.data.remote.dto.UpdateProfileRequest
import com.sunflower.pinpic.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
    private val okHttpClient: OkHttpClient,
    private val dataStoreManager: DataStoreManager,
    private val imageUploader: ImageUploader
) : UserRepository {

    override suspend fun updateProfile(
        fullName: String,
        mobileNumber: String,
        locale: String,
        pictureUrl: String,
        selectedUri: Uri?,
        imageName: String?
    ): Result<Unit> {
        return try {
            val token = dataStoreManager.jwtTokenFlow.firstOrNull()
                ?: return Result.Error("Not logged in")
            val bearer = "Bearer $token"

            var finalPictureUrl = pictureUrl

            if (selectedUri!=null && imageName!=null) {
                // 1. Initiate upload
                val initResponse = userApi.initiateUpload(bearer, InitiateUploadRequest(name = imageName))
                if (!initResponse.isSuccessful) return Result.Error("Failed to initiate upload")

                val initData = initResponse.body() ?: return Result.Error("Empty initiate response")
                val s3Key = initData.s3Key
                val presignedUrl = initData.presignedUrl

                // 2. Upload to S3
                val success = imageUploader.uploadPhotoToPresignedUrl(presignedUrl, selectedUri)
                if (!success) return Result.Error("Upload failed")

                // 3. Confirm upload
                val confirmResponse = userApi.confirmUpload(bearer, ConfirmUploadRequest(s3Key = s3Key))
                if (!confirmResponse.isSuccessful) return Result.Error("Failed to confirm upload")

                finalPictureUrl = confirmResponse.body()?.photoUrl ?: return Result.Error("Empty confirm response")
            }
            
            // 4. Update Profile
            val updateResponse = userApi.updateProfile(
                bearerToken = bearer,
                request = UpdateProfileRequest(
                    name = fullName,
                    mobileNumber = mobileNumber,
                    locale = locale,
                    pictureUrl = finalPictureUrl
                )
            )
            
            if (!updateResponse.isSuccessful) return Result.Error("Failed to update profile")
            
            // Update datastore
            dataStoreManager.updateProfileInfo(
                fullName = fullName,
                email = null,
                mobileNumber = mobileNumber,
                pictureUrl = finalPictureUrl
            )
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun searchUsers(query: String): Result<List<com.sunflower.pinpic.data.remote.dto.UserProfileResponse>> {
        return try {
            val token = dataStoreManager.jwtTokenFlow.firstOrNull()
                ?: return Result.Error("Not logged in")
            val response = userApi.searchUsers("Bearer $token", query)
            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Error("Failed to search users")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun getUserProfile(userId: String): Result<com.sunflower.pinpic.data.remote.dto.UserProfileResponse> {
        return try {
            val token = dataStoreManager.jwtTokenFlow.firstOrNull()
                ?: return Result.Error("Not logged in")
            val response = userApi.getUserProfile("Bearer $token", userId)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error("Failed to load user profile")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun getUserPhotos(userId: String): Result<List<com.sunflower.pinpic.data.remote.dto.UserPhotoResponse>> {
        return try {
            val token = dataStoreManager.jwtTokenFlow.firstOrNull()
                ?: return Result.Error("Not logged in")
            val response = userApi.getUserPhotos("Bearer $token", userId)
            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Error("Failed to load user photos")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun followUser(userId: String): Result<Unit> {
        return try {
            val token = dataStoreManager.jwtTokenFlow.firstOrNull()
                ?: return Result.Error("Not logged in")
            val response = userApi.followUser("Bearer $token", userId)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error("Failed to follow user")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun unfollowUser(userId: String): Result<Unit> {
        return try {
            val token = dataStoreManager.jwtTokenFlow.firstOrNull()
                ?: return Result.Error("Not logged in")
            val response = userApi.unfollowUser("Bearer $token", userId)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error("Failed to unfollow user")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }
}
