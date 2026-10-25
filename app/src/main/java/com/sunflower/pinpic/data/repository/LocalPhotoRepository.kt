package com.sunflower.pinpic.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.sunflower.pinpic.common.Result
import com.sunflower.pinpic.data.local.dao.PhotoDao
import com.sunflower.pinpic.data.local.dao.SyncOperationDao
import com.sunflower.pinpic.data.local.entity.EntityType
import com.sunflower.pinpic.data.local.entity.Operations
import com.sunflower.pinpic.data.local.entity.Status
import com.sunflower.pinpic.data.local.entity.SyncOperationEntity
import com.sunflower.pinpic.data.local.entity.Visibility
import com.sunflower.pinpic.data.mapper.toDomain
import com.sunflower.pinpic.data.mapper.toEntity
import com.sunflower.pinpic.domain.model.Cluster
import com.sunflower.pinpic.domain.model.Photo
import com.sunflower.pinpic.domain.repository.PhotoRepository
import com.sunflower.pinpic.worker.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class LocalPhotoRepository @Inject constructor(
    private val dao: PhotoDao,
    private val syncOperationDao: SyncOperationDao,
    private val syncScheduler: SyncScheduler
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

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun savePhoto(photo: Photo): Int {
        val id = dao.insertPhoto(photo.toEntity()).toInt()
        val now = Instant.now().toString()
        syncOperationDao.insertOperation(
            SyncOperationEntity(
                entityType = EntityType.PHOTOS,
                entityId = id.toString(),
                operation = Operations.ADD,
                status = Status.PENDING,
                createdAt = now
            )
        )
        syncScheduler.scheduleSync()
        return id
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun savePhotos(photos: List<Photo>): List<Int> {
        val ids = photos.map { dao.insertPhoto(it.toEntity()).toInt() }
        val now = Instant.now().toString()
        val operations = ids.map { id ->
            SyncOperationEntity(
                entityType = EntityType.PHOTOS,
                entityId = id.toString(),
                operation = Operations.ADD,
                status = Status.PENDING,
                createdAt = now
            )
        }
        syncOperationDao.insertOperations(operations)
        syncScheduler.scheduleSync()
        return ids
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun updatePhoto(photo: Photo): Boolean {
        return try {
            dao.updatePhoto(photo.toEntity())
            val now = Instant.now().toString()
            syncOperationDao.insertOperation(
                SyncOperationEntity(
                    entityType = EntityType.PHOTOS,
                    entityId = photo.localPhotoId.toString(),
                    operation = Operations.UPDATE,
                    status = Status.PENDING,
                    createdAt = now
                )
            )
            syncScheduler.scheduleSync()
            true
        } catch (_: Exception) {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun updatePhotosVisibility(photos: List<Photo>, visibility: Visibility): Boolean {
        if (photos.isEmpty()) return true
        return try {
            val now = Instant.now().toString()
            val photoIds = photos.map { it.localPhotoId }
            dao.updatePhotosVisibility(photoIds, visibility, now)
            val operations = photoIds.map { id ->
                SyncOperationEntity(
                    entityType = EntityType.PHOTOS,
                    entityId = id.toString(),
                    operation = Operations.UPDATE,
                    status = Status.PENDING,
                    createdAt = now
                )
            }
            syncOperationDao.insertOperations(operations)
            syncScheduler.scheduleSync()
            true
        } catch (_: Exception) {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun saveCluster(cluster: Cluster): Int {
        val id = dao.insertCluster(cluster.toEntity()).toInt()
        val now = Instant.now().toString()
        syncOperationDao.insertOperation(
            SyncOperationEntity(
                entityType = EntityType.CLUSTER,
                entityId = id.toString(),
                operation = Operations.ADD,
                status = Status.PENDING,
                createdAt = now
            )
        )
        syncScheduler.scheduleSync()
        return id
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun updateCluster(cluster: Cluster): Boolean {
        return try {
            dao.updateCluster(cluster.toEntity())
            val now = Instant.now().toString()
            syncOperationDao.insertOperation(
                SyncOperationEntity(
                    entityType = EntityType.CLUSTER,
                    entityId = cluster.localClusterId.toString(),
                    operation = Operations.UPDATE,
                    status = Status.PENDING,
                    createdAt = now
                )
            )
            syncScheduler.scheduleSync()
            true
        } catch (_: Exception) {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun deletePhotos(photos: List<Photo>): Boolean {
        return try {
            val now = Instant.now().toString()
            val operations = photos.map { photo ->
                SyncOperationEntity(
                    entityType = EntityType.PHOTOS,
                    entityId = photo.localPhotoId.toString(),
                    operation = Operations.DELETE,
                    status = Status.PENDING,
                    createdAt = now
                )
            }
            dao.deletePhotosAndCleanupClusters(photos.map { it.toEntity() })
            syncOperationDao.insertOperations(operations)
            syncScheduler.scheduleSync()
            true
        } catch (_: Exception) {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun addPhotosInCluster(photos: List<Photo>, cluster: Cluster): Boolean {
        return try {
            val clusterId = cluster.localClusterId
            val photoIds = photos.map { it.localPhotoId }
            dao.updateClusterIdForPhotos(photoIds, clusterId)
            val now = Instant.now().toString()
            val operations = photoIds.map { id ->
                SyncOperationEntity(
                    entityType = EntityType.PHOTOS,
                    entityId = id.toString(),
                    operation = Operations.UPDATE,
                    status = Status.PENDING,
                    createdAt = now
                )
            }
            syncOperationDao.insertOperations(operations)
            syncScheduler.scheduleSync()
            true
        } catch (_: Exception) {
            false
        }
    }
}
