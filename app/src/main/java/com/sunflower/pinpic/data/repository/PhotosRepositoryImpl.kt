package com.sunflower.pinpic.data.repository

import com.sunflower.pinpic.data.local.dao.PhotoDao
import com.sunflower.pinpic.data.mapper.toDomain
import com.sunflower.pinpic.data.mapper.toEntity
import com.sunflower.pinpic.data.local.entity.PhotoEntity
import com.sunflower.pinpic.domain.model.Photo
import com.sunflower.pinpic.domain.repository.PhotosRepository
import com.sunflower.pinpic.common.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
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

    override fun getAllPhotosFlow(): Flow<Result<List<Photo>>> {
        return dao.getAllPhotosFlow().map<List<PhotoEntity>, Result<List<Photo>>> { entities ->
            Result.Success(entities.map { it.toDomain() })
        }.catch { e ->
            emit(Result.Error(e.message ?: "Unknown error"))
        }
    }

    override suspend fun deletePhotos(photos: List<Photo>): Result<Unit> {
        return try {
            dao.deletePhotos(photos.map { it.toEntity() })
            // Delete actual files
            photos.forEach { photo ->
                photo.uri?.let { uriString ->
                    val file = java.io.File(uriString)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }
}
