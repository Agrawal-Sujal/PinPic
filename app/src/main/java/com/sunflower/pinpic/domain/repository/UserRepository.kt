package com.sunflower.pinpic.domain.repository

import com.sunflower.pinpic.common.Result

interface UserRepository {
    suspend fun updateProfile(
        fullName: String,
        mobileNumber: String,
        locale: String,
        pictureUrl: String,
        imageBytes: ByteArray?,
        imageName: String?
    ): Result<Unit>
}
