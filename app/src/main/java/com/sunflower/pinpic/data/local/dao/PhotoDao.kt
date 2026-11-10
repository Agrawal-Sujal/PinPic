package com.sunflower.pinpic.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.sunflower.pinpic.data.local.entity.ClusterEntity
import com.sunflower.pinpic.data.local.entity.PhotoEntity
import com.sunflower.pinpic.data.local.entity.Visibility
import kotlinx.coroutines.flow.Flow

data class ClusterCleanupResult(
    val softDeletedClusterIds: List<Int> = emptyList(),
    val updatedClusterIds: List<Int> = emptyList()
)

@Dao
interface PhotoDao {
    @Insert
    @JvmSuppressWildcards
    suspend fun insertPhoto(photo: PhotoEntity): Long

    @Update
    suspend fun updatePhoto(photo: PhotoEntity)

    @Query("SELECT * FROM photos WHERE isDeleted = 0 ORDER BY localPhotoId DESC")
    suspend fun getAllPhotos(): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE isDeleted = 0 ORDER BY localPhotoId DESC")
    fun getAllPhotosFlow(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE clusterId IS NULL AND isDeleted = 0 ORDER BY localPhotoId DESC")
    suspend fun getUnclusteredPhotos(): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE clusterId IS NULL AND isDeleted = 0 ORDER BY localPhotoId DESC")
    fun getUnclusteredPhotosFlow(): Flow<List<PhotoEntity>>

    @Insert
    suspend fun insertCluster(cluster: ClusterEntity): Long

    @Update
    suspend fun updateCluster(cluster: ClusterEntity)

    @Query("SELECT * FROM clusters WHERE isDeleted = 0")
    suspend fun getAllClusters(): List<ClusterEntity>

    @Query("SELECT * FROM clusters WHERE isDeleted = 0")
    fun getAllClustersFlow(): Flow<List<ClusterEntity>>

    @Query("SELECT * FROM photos WHERE clusterId IN (:clusterIds) AND isDeleted = 0")
    suspend fun getPhotosByClusterIds(clusterIds: List<Int>): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE localPhotoId IN (:photoIds) AND isDeleted = 0")
    suspend fun getPhotosByIds(photoIds: List<Int>): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE localPhotoId = :photoId LIMIT 1")
    suspend fun getPhotoById(photoId: Int): PhotoEntity?

    @Query("SELECT * FROM photos WHERE globalPhotoId = :globalPhotoId LIMIT 1")
    suspend fun getPhotoByGlobalPhotoId(globalPhotoId: String): PhotoEntity?

    @Query("DELETE FROM photos WHERE globalPhotoId = :globalPhotoId")
    suspend fun deletePhotoByGlobalId(globalPhotoId: String): Int

    @Query("DELETE FROM photos WHERE localPhotoId = :photoId")
    suspend fun purgePhotoById(photoId: Int): Int

    @Query("DELETE FROM photos WHERE localPhotoId IN (:photoIds)")
    suspend fun purgePhotosByIds(photoIds: List<Int>): Int

    @Query("UPDATE photos SET isDeleted = 1 WHERE localPhotoId IN (:photoIds)")
    suspend fun softDeletePhotos(photoIds: List<Int>): Int

    @Query("UPDATE photos SET globalPhotoId = :globalPhotoId WHERE localPhotoId = :localPhotoId")
    suspend fun updatePhotoGlobalId(localPhotoId: Int, globalPhotoId: String)

    @Query("UPDATE photos SET s3Key = :s3Key WHERE localPhotoId = :localPhotoId")
    suspend fun updatePhotoS3Key(localPhotoId: Int, s3Key: String)

    @Query("UPDATE photos SET visibility = :visibility, editedAt = :editedAt WHERE localPhotoId IN (:photoIds)")
    suspend fun updatePhotosVisibility(photoIds: List<Int>, visibility: Visibility, editedAt: String)

    @Query("SELECT * FROM photos WHERE clusterId = :clusterId AND isDeleted = 0 LIMIT 1")
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

    @Query("DELETE FROM clusters WHERE localClusterId = :clusterId")
    suspend fun purgeClusterById(clusterId: Int): Int

    @Query("DELETE FROM clusters WHERE localClusterId IN (:clusterIds)")
    suspend fun purgeClustersByIds(clusterIds: List<Int>): Int

    @Query("UPDATE clusters SET isDeleted = 1 WHERE localClusterId IN (:clusterIds)")
    suspend fun softDeleteClusters(clusterIds: List<Int>): Int

    @Update
    suspend fun updateClusters(clusters: List<ClusterEntity>)

    @Query("UPDATE photos SET clusterId = :clusterId WHERE localPhotoId IN (:photoIds)")
    suspend fun updateClusterIdForPhotos(photoIds: List<Int>, clusterId: Int?)

    @Query("SELECT DISTINCT clusterId FROM photos WHERE localPhotoId IN (:photoIds) AND clusterId IS NOT NULL")
    suspend fun getClusterIdsForPhotoIds(photoIds: List<Int>): List<Int>

    @Query("SELECT COUNT(*) FROM photos WHERE clusterId = :clusterId AND isDeleted = 0")
    suspend fun getPhotoCountForCluster(clusterId: Int): Int

    @Query("SELECT COUNT(*) FROM photos WHERE clusterId = :clusterId AND visibility = :visibility AND isDeleted = 0")
    suspend fun getPhotoCountForClusterAndVisibility(clusterId: Int, visibility: Visibility): Int

    @Transaction
    suspend fun insertClusterAndPhotos(cluster: ClusterEntity, photos: List<PhotoEntity>): Long {
        val publicCount = photos.count { it.visibility == Visibility.PUBLIC }
        val friendCount = photos.count { it.visibility == Visibility.FRIENDS }
        val privateCount = photos.count { it.visibility == Visibility.PRIVATE }
        val visibility = when {
            publicCount > 0 -> Visibility.PUBLIC
            friendCount > 0 -> Visibility.FRIENDS
            else -> Visibility.PRIVATE
        }
        val clusterToInsert = cluster.copy(
            photoCount = photos.size,
            visibility = visibility,
            publicPhotoCount = publicCount,
            privatePhotoCount = privateCount,
            friendPhotoCount = friendCount
        )
        val clusterId = insertCluster(clusterToInsert).toInt()
        photos.forEach { 
            insertPhoto(it.copy(clusterId = clusterId))
        }
        return clusterId.toLong()
    }

    @Transaction
    suspend fun deletePhotosAndCleanupClusters(photos: List<PhotoEntity>): ClusterCleanupResult {
        if (photos.isEmpty()) return ClusterCleanupResult()
        val photoIds = photos.map { it.localPhotoId }

        val affectedClusterIds = getClusterIdsForPhotoIds(photoIds)

        // Soft delete the photos
        softDeletePhotos(photoIds)

        val softDeletedClusterIds = mutableListOf<Int>()
        val updatedClusterIds = mutableListOf<Int>()

        if (affectedClusterIds.isNotEmpty()) {
            val affectedClusters = getClustersByIds(affectedClusterIds)
            val clustersToSoftDelete = mutableListOf<Int>()
            val clustersToUpdate = mutableListOf<ClusterEntity>()

            for (cluster in affectedClusters) {
                val remainingCount = getPhotoCountForCluster(cluster.localClusterId)
                if (remainingCount == 0) {
                    clustersToSoftDelete.add(cluster.localClusterId)
                } else {
                    val newThumbnailId = if (cluster.thumbnailId in photoIds || cluster.thumbnailId == null) {
                        getFirstPhotoForCluster(cluster.localClusterId)?.localPhotoId
                    } else {
                        cluster.thumbnailId
                    }
                    val publicCount = getPhotoCountForClusterAndVisibility(
                        cluster.localClusterId,
                        Visibility.PUBLIC
                    )
                    val friendCount = getPhotoCountForClusterAndVisibility(
                        cluster.localClusterId,
                        Visibility.FRIENDS
                    )
                    val privateCount = getPhotoCountForClusterAndVisibility(
                        cluster.localClusterId,
                        Visibility.PRIVATE
                    )
                    val visibility = when {
                        publicCount > 0 -> Visibility.PUBLIC
                        friendCount > 0 -> Visibility.FRIENDS
                        else -> Visibility.PRIVATE
                    }
                    clustersToUpdate.add(
                        cluster.copy(
                            photoCount = remainingCount,
                            thumbnailId = newThumbnailId,
                            visibility = visibility,
                            publicPhotoCount = publicCount,
                            privatePhotoCount = privateCount,
                            friendPhotoCount = friendCount
                        )
                    )
                    updatedClusterIds.add(cluster.localClusterId)
                }
            }

            if (clustersToSoftDelete.isNotEmpty()) {
                softDeleteClusters(clustersToSoftDelete)
                softDeletedClusterIds.addAll(clustersToSoftDelete)
            }
            if (clustersToUpdate.isNotEmpty()) {
                updateClusters(clustersToUpdate)
            }
        }

        return ClusterCleanupResult(
            softDeletedClusterIds = softDeletedClusterIds,
            updatedClusterIds = updatedClusterIds
        )
    }

    @Transaction
    suspend fun purgePhotoAndCleanupClusters(photo: PhotoEntity) {
        val clusterId = photo.clusterId
        purgePhotoById(photo.localPhotoId)
        if (clusterId != null) {
            val cluster = getClusterById(clusterId)
            if (cluster != null) {
                val remainingCount = getPhotoCountForCluster(clusterId)
                if (remainingCount == 0) {
                    purgeClusterById(clusterId)
                } else {
                    val newThumbnailId = if (cluster.thumbnailId == photo.localPhotoId || cluster.thumbnailId == null) {
                        getFirstPhotoForCluster(clusterId)?.localPhotoId
                    } else {
                        cluster.thumbnailId
                    }
                    val publicCount = getPhotoCountForClusterAndVisibility(clusterId, Visibility.PUBLIC)
                    val friendCount = getPhotoCountForClusterAndVisibility(clusterId, Visibility.FRIENDS)
                    val privateCount = getPhotoCountForClusterAndVisibility(clusterId, Visibility.PRIVATE)
                    val visibility = when {
                        publicCount > 0 -> Visibility.PUBLIC
                        friendCount > 0 -> Visibility.FRIENDS
                        else -> Visibility.PRIVATE
                    }
                    updateCluster(
                        cluster.copy(
                            photoCount = remainingCount,
                            thumbnailId = newThumbnailId,
                            visibility = visibility,
                            publicPhotoCount = publicCount,
                            privatePhotoCount = privateCount,
                            friendPhotoCount = friendCount
                        )
                    )
                }
            }
        }
    }
}
