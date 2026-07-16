package com.sunflower.pinpic.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sunflower.pinpic.data.local.entity.ClusterPointEntity
import com.sunflower.pinpic.data.local.entity.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @JvmSuppressWildcards
    suspend fun insertPhoto(photo: PhotoEntity): Long

    @Query("SELECT * FROM photos ORDER BY id DESC")
    suspend fun getAllPhotos(): List<PhotoEntity>

    @Query("SELECT * FROM photos ORDER BY id DESC")
    fun getAllPhotosFlow(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE clusterId IS NULL ORDER BY id DESC")
    suspend fun getUnclusteredPhotos(): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE clusterId IS NULL ORDER BY id DESC")
    fun getUnclusteredPhotosFlow(): Flow<List<PhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClusterPoint(clusterPoint: ClusterPointEntity): Long

    @Query("SELECT * FROM cluster_points")
    suspend fun getAllClusterPoints(): List<ClusterPointEntity>

    @Query("SELECT * FROM cluster_points")
    fun getAllClusterPointsFlow(): Flow<List<ClusterPointEntity>>

    @Query("SELECT * FROM photos WHERE clusterId IN (:clusterIds)")
    suspend fun getPhotosByClusterIds(clusterIds: List<Long>): List<PhotoEntity>

    @androidx.room.Delete
    suspend fun deletePhotos(photos: List<PhotoEntity>)

    @androidx.room.Transaction
    suspend fun insertClusterAndPhotos(cluster: ClusterPointEntity, photos: List<PhotoEntity>): Long {
        val clusterId = insertClusterPoint(cluster)
        val photosWithCluster = photos.map { it.copy(clusterId = clusterId) }
        photosWithCluster.forEach { insertPhoto(it) }
        return clusterId
    }
}