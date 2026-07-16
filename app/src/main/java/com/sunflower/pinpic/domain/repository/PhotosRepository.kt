package com.sunflower.pinpic.domain.repository

import com.sunflower.pinpic.domain.model.ClusterPoint
import com.sunflower.pinpic.domain.model.Photo
import com.sunflower.pinpic.common.Result
import kotlinx.coroutines.flow.Flow

interface PhotosRepository {
    suspend fun savePhoto(photo: Photo): Result<Unit>
    suspend fun getAllPhotos(): Result<List<Photo>>
    fun getAllPhotosFlow(): Flow<Result<List<Photo>>>
    suspend fun getUnclusteredPhotos(): Result<List<Photo>>
    fun getUnclusteredPhotosFlow(): Flow<Result<List<Photo>>>
    suspend fun getPhotosByClusterIds(clusterIds: List<Long>): Result<List<Photo>>
    suspend fun saveClusterPoint(clusterPoint: ClusterPoint): Result<Long>
    suspend fun saveClusterWithPhotos(clusterPoint: ClusterPoint, photos: List<Photo>): Result<Unit>
    suspend fun getAllClusterPoints(): Result<List<ClusterPoint>>
    fun getAllClusterPointsFlow(): Flow<Result<List<ClusterPoint>>>
    suspend fun deletePhotos(photos: List<Photo>): Result<Unit>
}
