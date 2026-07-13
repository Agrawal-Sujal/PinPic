package com.sunflower.pinpic.data.repository

import com.sunflower.pinpic.data.local.dao.PhotoDao
import com.sunflower.pinpic.data.mapper.toDomain
import com.sunflower.pinpic.data.mapper.toEntity
import com.sunflower.pinpic.domain.model.Photo
import com.sunflower.pinpic.domain.repository.PhotosRepository
import com.sunflower.pinpic.common.Result
import javax.inject.Inject

class PhotosRepositoryImpl @Inject constructor(
    private val dao: PhotoDao
) : PhotosRepository {

    override suspend fun savePhoto(photo: Photo): Result<Unit> {
        return try {
            dao.insertPhoto(photo.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun getAllPhotos(): Result<List<Photo>>{
        return try {
            val photos = dao.getAllPhotos().map { it.toDomain() }
            Result.Success(photos)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }
}
