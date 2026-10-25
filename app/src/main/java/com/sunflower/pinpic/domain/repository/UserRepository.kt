package com.sunflower.pinpic.domain.repository

import android.net.Uri
import com.sunflower.pinpic.common.Result
import com.sunflower.pinpic.data.remote.dto.UserProfileResponse

interface UserRepository {
    suspend fun updateProfile(
        fullName: String,
        mobileNumber: String,
        locale: String,
        pictureUrl: String,
        selectedUri: Uri?,
        imageName: String?
    ): Result<Unit>

    suspend fun searchUsers(query: String): Result<List<UserProfileResponse>>
    suspend fun getUserProfile(userId: String): Result<UserProfileResponse>
    suspend fun getUserPhotos(userId: String): Result<List<com.sunflower.pinpic.data.remote.dto.UserPhotoResponse>>
    suspend fun followUser(userId: String): Result<Unit>
    suspend fun unfollowUser(userId: String): Result<Unit>
}
