package com.sunflower.pinpic.data.repository

import com.sunflower.pinpic.common.Result
import com.sunflower.pinpic.domain.model.Cluster
import com.sunflower.pinpic.domain.model.Photo
import com.sunflower.pinpic.domain.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RemotePhotoRepository @Inject constructor() : PhotoRepository {
    override fun getAllPhotosFlow(): Flow<Result<List<Photo>>> {
        TODO("Not yet implemented")
    }

    override suspend fun getUnMergedCluster(): List<Cluster> {
        TODO("Not yet implemented")
    }

    override suspend fun getPhotosOfCluster(clusterId: Int): List<Photo> {
        TODO("Not yet implemented")
    }

    override suspend fun savePhoto(photo: Photo): Int {
        TODO("Not yet implemented")
    }

    override suspend fun savePhotos(photos: List<Photo>): List<Int> {
        TODO("Not yet implemented")
    }

    override suspend fun updatePhoto(photo: Photo): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun saveCluster(cluster: Cluster): Int {
        TODO("Not yet implemented")
    }

    override suspend fun updateCluster(cluster: Cluster): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun deletePhotos(photos: List<Photo>): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun addPhotosInCluster(photos: List<Photo>, cluster: Cluster): Boolean {
        TODO("Not yet implemented")
    }
}
