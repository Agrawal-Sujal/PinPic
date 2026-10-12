package com.sunflower.pinpic.data.repository

import com.sunflower.pinpic.common.Result
import com.sunflower.pinpic.data.local.dao.PhotoDao
import com.sunflower.pinpic.data.local.entity.ClusterPhotosEntity
import com.sunflower.pinpic.data.mapper.toDomain
import com.sunflower.pinpic.data.mapper.toEntity
import com.sunflower.pinpic.domain.model.Cluster
import com.sunflower.pinpic.domain.model.Photo
import com.sunflower.pinpic.domain.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalPhotoRepository @Inject constructor(
    private val dao: PhotoDao
) : PhotoRepository {

    override fun getAllPhotosFlow(): Flow<Result<List<Photo>>> {
        return dao.getAllPhotosFlow().map { entities ->
            try {
                Result.Success(entities.map { it.toDomain() })
            } catch (e: Exception) {
                Result.Error(e.message ?: "An error occurred")
            }
        }
    }

    override suspend fun getUnMergedCluster(): List<Cluster> {
        val clusters = dao.getAllClusters()
        if (clusters.isEmpty()) return emptyList()

        val thumbnailIds = clusters.mapNotNull { it.thumbnailId }.distinct()
        val photosMap = if (thumbnailIds.isNotEmpty()) {
            dao.getPhotosByIds(thumbnailIds).associateBy { it.localPhotoId }
        } else {
            emptyMap()
        }

        return clusters.map { entity ->
            val thumbnailEntity = entity.thumbnailId?.let { photosMap[it] }
                ?: dao.getFirstPhotoForCluster(entity.localClusterId)
            entity.toDomain(thumbnail = thumbnailEntity?.toDomain())
        }
    }

    override suspend fun getPhotosOfCluster(clusterId: Int): List<Photo> {
        val photos = dao.getPhotosByClusterIds(listOf(clusterId))
        return photos.map { it.toDomain() }
    }

    override suspend fun savePhoto(photo: Photo): Int {
        return dao.insertPhoto(photo.toEntity()).toInt()
    }

    override suspend fun savePhotos(photos: List<Photo>): List<Int> {
        return photos.map { dao.insertPhoto(it.toEntity()).toInt() }
    }

    override suspend fun updatePhoto(photo: Photo): Boolean {
        return try {
            savePhoto(photo)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun saveCluster(cluster: Cluster): Int {
        return dao.insertCluster(cluster.toEntity()).toInt()
    }

    override suspend fun deletePhotos(photos: List<Photo>): Boolean {
        return try {
            dao.deletePhotos(photos.map { it.toEntity() })
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun addPhotosInCluster(photos: List<Photo>, cluster: Cluster): Boolean {
        return try {
            val clusterId = cluster.localClusterId
            photos.forEach { photo ->
                dao.insertClusterPhoto(ClusterPhotosEntity(photoId = photo.localPhotoId, clusterId = clusterId))
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
