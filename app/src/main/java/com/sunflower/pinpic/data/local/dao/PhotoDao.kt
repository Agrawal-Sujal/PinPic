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
import com.sunflower.pinpic.data.local.entity.Visibility
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Insert
    @JvmSuppressWildcards
    suspend fun insertPhoto(photo: PhotoEntity): Long

    @Update
    suspend fun updatePhoto(photo: PhotoEntity)

    @Query("SELECT * FROM photos ORDER BY localPhotoId DESC")
    suspend fun getAllPhotos(): List<PhotoEntity>

    @Query("SELECT * FROM photos ORDER BY localPhotoId DESC")
    fun getAllPhotosFlow(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE clusterId IS NULL ORDER BY localPhotoId DESC")
    suspend fun getUnclusteredPhotos(): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE clusterId IS NULL ORDER BY localPhotoId DESC")
    fun getUnclusteredPhotosFlow(): Flow<List<PhotoEntity>>

    @Insert
    suspend fun insertCluster(cluster: ClusterEntity): Long

    @Update
    suspend fun updateCluster(cluster: ClusterEntity)

    @Query("SELECT * FROM clusters")
    suspend fun getAllClusters(): List<ClusterEntity>

    @Query("SELECT * FROM clusters")
    fun getAllClustersFlow(): Flow<List<ClusterEntity>>

    @Query("SELECT * FROM photos WHERE clusterId IN (:clusterIds)")
    suspend fun getPhotosByClusterIds(clusterIds: List<Int>): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE localPhotoId IN (:photoIds)")
    suspend fun getPhotosByIds(photoIds: List<Int>): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE localPhotoId = :photoId LIMIT 1")
    suspend fun getPhotoById(photoId: Int): PhotoEntity?

    @Query("SELECT * FROM photos WHERE globalPhotoId = :globalPhotoId LIMIT 1")
    suspend fun getPhotoByGlobalPhotoId(globalPhotoId: String): PhotoEntity?

    @Query("DELETE FROM photos WHERE globalPhotoId = :globalPhotoId")
    suspend fun deletePhotoByGlobalId(globalPhotoId: String): Int

    @Query("UPDATE photos SET globalPhotoId = :globalPhotoId WHERE localPhotoId = :localPhotoId")
    suspend fun updatePhotoGlobalId(localPhotoId: Int, globalPhotoId: String)

    @Query("UPDATE photos SET s3Key = :s3Key WHERE localPhotoId = :localPhotoId")
    suspend fun updatePhotoS3Key(localPhotoId: Int, s3Key: String)

    @Query("UPDATE photos SET visibility = :visibility, editedAt = :editedAt WHERE localPhotoId IN (:photoIds)")
    suspend fun updatePhotosVisibility(photoIds: List<Int>, visibility: Visibility, editedAt: String)

    @Query("SELECT * FROM photos WHERE clusterId = :clusterId LIMIT 1")
    suspend fun getFirstPhotoForCluster(clusterId: Int): PhotoEntity?

    @Delete
    suspend fun deletePhotos(photos: List<PhotoEntity>)

    @Query("SELECT * FROM clusters WHERE localClusterId IN (:clusterIds)")
    suspend fun getClustersByIds(clusterIds: List<Int>): List<ClusterEntity>

    @Query("SELECT * FROM clusters WHERE localClusterId = :clusterId LIMIT 1")
    suspend fun getClusterById(clusterId: Int): ClusterEntity?

    @Query("SELECT * FROM clusters WHERE globalClusterId = :globalClusterId LIMIT 1")
    suspend fun getClusterByGlobalClusterId(globalClusterId: String): ClusterEntity?

    @Query("UPDATE clusters SET globalClusterId = :globalClusterId WHERE localClusterId = :localClusterId")
    suspend fun updateClusterGlobalId(localClusterId: Int, globalClusterId: String)

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