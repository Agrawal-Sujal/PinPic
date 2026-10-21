package com.sunflower.pinpic.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.sunflower.pinpic.data.local.entity.ClusterEntity
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

    @Query("SELECT * FROM photos WHERE clusterId IS NULL ORDER BY localPhotoId DESC")
    suspend fun getUnclusteredPhotos(): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE clusterId IS NULL ORDER BY localPhotoId DESC")
    fun getUnclusteredPhotosFlow(): Flow<List<PhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCluster(cluster: ClusterEntity): Long

    @Query("SELECT * FROM clusters")
    suspend fun getAllClusters(): List<ClusterEntity>

    @Query("SELECT * FROM clusters")
    fun getAllClustersFlow(): Flow<List<ClusterEntity>>

    @Query("SELECT * FROM photos WHERE clusterId IN (:clusterIds)")
    suspend fun getPhotosByClusterIds(clusterIds: List<Int>): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE localPhotoId IN (:photoIds)")
    suspend fun getPhotosByIds(photoIds: List<Int>): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE clusterId = :clusterId LIMIT 1")
    suspend fun getFirstPhotoForCluster(clusterId: Int): PhotoEntity?

    @Delete
    suspend fun deletePhotos(photos: List<PhotoEntity>)

    @Query("SELECT * FROM clusters WHERE localClusterId IN (:clusterIds)")
    suspend fun getClustersByIds(clusterIds: List<Int>): List<ClusterEntity>

    @Delete
    suspend fun deleteClusters(clusters: List<ClusterEntity>)

    @Update
    suspend fun updateClusters(clusters: List<ClusterEntity>)

    @Query("UPDATE photos SET clusterId = :clusterId WHERE localPhotoId IN (:photoIds)")
    suspend fun updateClusterIdForPhotos(photoIds: List<Int>, clusterId: Int?)

    @Query("SELECT DISTINCT clusterId FROM photos WHERE localPhotoId IN (:photoIds) AND clusterId IS NOT NULL")
    suspend fun getClusterIdsForPhotoIds(photoIds: List<Int>): List<Int>

    @Query("SELECT COUNT(*) FROM photos WHERE clusterId = :clusterId")
    suspend fun getPhotoCountForCluster(clusterId: Int): Int

    @Query("DELETE FROM clusters WHERE localClusterId NOT IN (SELECT DISTINCT clusterId FROM photos WHERE clusterId IS NOT NULL)")
    suspend fun deleteEmptyClusters()

    @Transaction
    suspend fun insertClusterAndPhotos(cluster: ClusterEntity, photos: List<PhotoEntity>): Long {
        val clusterId = insertCluster(cluster).toInt()
        photos.forEach { 
            insertPhoto(it.copy(clusterId = clusterId))
        }
        return clusterId.toLong()
    }

    @Transaction
    suspend fun deletePhotosAndCleanupClusters(photos: List<PhotoEntity>) {
        if (photos.isEmpty()) return
        val photoIds = photos.map { it.localPhotoId }

        val affectedClusterIds = getClusterIdsForPhotoIds(photoIds)

        deletePhotos(photos)

        if (affectedClusterIds.isNotEmpty()) {
            val affectedClusters = getClustersByIds(affectedClusterIds)
            val clustersToDelete = mutableListOf<ClusterEntity>()
            val clustersToUpdate = mutableListOf<ClusterEntity>()

            for (cluster in affectedClusters) {
                val remainingCount = getPhotoCountForCluster(cluster.localClusterId)
                if (remainingCount == 0) {
                    clustersToDelete.add(cluster)
                } else {
                    val newThumbnailId = if (cluster.thumbnailId in photoIds || cluster.thumbnailId == null) {
                        getFirstPhotoForCluster(cluster.localClusterId)?.localPhotoId
                    } else {
                        cluster.thumbnailId
                    }
                    clustersToUpdate.add(
                        cluster.copy(
                            photoCount = remainingCount,
                            thumbnailId = newThumbnailId
                        )
                    )
                }
            }

            if (clustersToDelete.isNotEmpty()) {
                deleteClusters(clustersToDelete)
            }
            if (clustersToUpdate.isNotEmpty()) {
                updateClusters(clustersToUpdate)
            }
        }

        deleteEmptyClusters()
    }
}