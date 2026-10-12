package com.sunflower.pinpic.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sunflower.pinpic.data.local.entity.ClusterEntity
import com.sunflower.pinpic.data.local.entity.ClusterPhotosEntity
import com.sunflower.pinpic.data.local.entity.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @JvmSuppressWildcards
    suspend fun insertPhoto(photo: PhotoEntity): Long

    @Query("SELECT * FROM photos ORDER BY localPhotoId DESC")
    suspend fun getAllPhotos(): List<PhotoEntity>

    @Query("SELECT * FROM photos ORDER BY localPhotoId DESC")
    fun getAllPhotosFlow(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE localPhotoId NOT IN (SELECT photoId FROM cluster_photos) ORDER BY localPhotoId DESC")
    suspend fun getUnclusteredPhotos(): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE localPhotoId NOT IN (SELECT photoId FROM cluster_photos) ORDER BY localPhotoId DESC")
    fun getUnclusteredPhotosFlow(): Flow<List<PhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCluster(cluster: ClusterEntity): Long

    @Query("SELECT * FROM clusters")
    suspend fun getAllClusters(): List<ClusterEntity>

    @Query("SELECT * FROM clusters")
    fun getAllClustersFlow(): Flow<List<ClusterEntity>>

    @Query("SELECT p.* FROM photos p INNER JOIN cluster_photos cp ON p.localPhotoId = cp.photoId WHERE cp.clusterId IN (:clusterIds)")
    suspend fun getPhotosByClusterIds(clusterIds: List<Int>): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE localPhotoId IN (:photoIds)")
    suspend fun getPhotosByIds(photoIds: List<Int>): List<PhotoEntity>

    @Query("SELECT p.* FROM photos p INNER JOIN cluster_photos cp ON p.localPhotoId = cp.photoId WHERE cp.clusterId = :clusterId LIMIT 1")
    suspend fun getFirstPhotoForCluster(clusterId: Int): PhotoEntity?

    @androidx.room.Delete
    suspend fun deletePhotos(photos: List<PhotoEntity>)

    @Query("SELECT * FROM clusters WHERE localClusterId IN (:clusterIds)")
    suspend fun getClustersByIds(clusterIds: List<Int>): List<ClusterEntity>

    @androidx.room.Delete
    suspend fun deleteClusters(clusters: List<ClusterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClusterPhoto(clusterPhoto: ClusterPhotosEntity)

    @androidx.room.Transaction
    suspend fun insertClusterAndPhotos(cluster: ClusterEntity, photos: List<PhotoEntity>): Long {
        val clusterId = insertCluster(cluster).toInt()
        photos.forEach { 
            val photoId = if (it.localPhotoId == 0) insertPhoto(it).toInt() else { insertPhoto(it); it.localPhotoId }
            insertClusterPhoto(ClusterPhotosEntity(photoId, clusterId))
        }
        return clusterId.toLong()
    }
}