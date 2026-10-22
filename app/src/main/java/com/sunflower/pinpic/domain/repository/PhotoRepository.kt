package com.sunflower.pinpic.domain.repository

import com.sunflower.pinpic.common.Result
import com.sunflower.pinpic.domain.model.Cluster
import com.sunflower.pinpic.domain.model.Photo
import kotlinx.coroutines.flow.Flow

interface PhotoRepository {
    fun getAllPhotosFlow(): Flow<Result<List<Photo>>>
    suspend fun getUnMergedCluster(): List<Cluster>
    suspend fun getPhotosOfCluster(clusterId: Int): List<Photo>
    suspend fun savePhoto(photo: Photo): Int
    suspend fun savePhotos(photos: List<Photo>): List<Int>
    suspend fun updatePhoto(photo: Photo): Boolean
    suspend fun saveCluster(cluster: Cluster): Int
    suspend fun updateCluster(cluster: Cluster): Boolean
    suspend fun deletePhotos(photos: List<Photo>): Boolean
    suspend fun addPhotosInCluster(photos:List<Photo>,cluster:Cluster): Boolean
}
