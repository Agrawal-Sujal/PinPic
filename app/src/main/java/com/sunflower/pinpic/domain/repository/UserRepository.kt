package com.sunflower.pinpic.domain.repository

import android.net.Uri
import com.sunflower.pinpic.common.Result

interface UserRepository {
    suspend fun updateProfile(
        fullName: String,
        mobileNumber: String,
        locale: String,
        pictureUrl: String,
        selectedUri: Uri?,
        imageName: String?
    ): Result<Unit>
}
