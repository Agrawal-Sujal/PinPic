package com.sunflower.pinpic.domain.repository

import com.sunflower.pinpic.domain.model.Photo
import com.sunflower.pinpic.common.Result
import kotlinx.coroutines.flow.Flow

interface PhotosRepository {
    suspend fun savePhoto(photo: Photo): Result<Unit>
    suspend fun getAllPhotos(): Result<List<Photo>>
    fun getAllPhotosFlow(): Flow<Result<List<Photo>>>
}
