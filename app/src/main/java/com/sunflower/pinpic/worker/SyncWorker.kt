package com.sunflower.pinpic.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.sunflower.pinpic.common.ImageUtils
import com.sunflower.pinpic.data.local.dao.PhotoDao
import com.sunflower.pinpic.data.local.dao.SyncOperationDao
import com.sunflower.pinpic.data.local.datastore.DataStoreManager
import com.sunflower.pinpic.data.local.entity.ClusterEntity
import com.sunflower.pinpic.data.local.entity.EntityType
import com.sunflower.pinpic.data.local.entity.Operations
import com.sunflower.pinpic.data.local.entity.PhotoEntity
import com.sunflower.pinpic.data.local.entity.Status
import com.sunflower.pinpic.data.remote.ImageDownloader
import com.sunflower.pinpic.data.remote.ImageUploader
import com.sunflower.pinpic.data.remote.api.PhotoApi
import com.sunflower.pinpic.data.remote.api.SyncApi
import com.sunflower.pinpic.data.remote.dto.ClusterSyncDto
import com.sunflower.pinpic.data.remote.dto.InitiateUploadRequest
import com.sunflower.pinpic.data.remote.dto.LocationSyncDto
import com.sunflower.pinpic.data.remote.dto.PhotoSyncDto
import com.sunflower.pinpic.data.remote.dto.SyncBatchRequest
import com.sunflower.pinpic.data.remote.dto.SyncItemDto
import com.sunflower.pinpic.data.remote.dto.SyncPullItemDto
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncOperationDao: SyncOperationDao,
    private val photoDao: PhotoDao,
    private val syncApi: SyncApi,
    private val photoApi: PhotoApi,
    private val imageUploader: ImageUploader,
    private val imageDownloader: ImageDownloader,
    private val dataStoreManager: DataStoreManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val channelId = "pinpic_sync_channel"
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "PinPic Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background photo synchronization"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("PinPic Sync")
            .setContentText("Syncing changes with cloud...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting immediate background sync operation...")
        return try {
            val token = dataStoreManager.jwtTokenFlow.first()
            if (token.isNullOrBlank()) {
                Log.w(TAG, "Sync deferred: No auth token found in DataStore. Please log in first.")
                return Result.success()
            }
            val authHeader = "Bearer $token"
            val deviceId = dataStoreManager.deviceIdFlow.first() ?: ""

            // 1. Push Phase: push local changes to the cloud
            val batchSize = 1
            while (true) {
//                delay(5000.milliseconds)
                val pendingBatch = syncOperationDao.getPendingOperationsBatch(batchSize)
                if (pendingBatch.isEmpty()) {
                    Log.i(TAG, "All pending push operations have been processed.")
                    break
                }
                Log.d(TAG, "Fetched batch of ${pendingBatch.size} pending operations.")

                val itemsToSend = mutableListOf<SyncItemDto>()
                var handledAnyLocally = false

                for (op in pendingBatch) {
                    val localId = op.entityId.toIntOrNull()
                    if (localId == null) {
                        Log.w(TAG, "Invalid local entity ID in sync operation: ${op.entityId}, skipping.")
                        syncOperationDao.updateStatus(op.operationId, Status.COMPLETED)
                        handledAnyLocally = true
                        continue
                    }

                    when (op.entityType) {
                        EntityType.CLUSTER -> {
                            if (op.operation == Operations.ADD || op.operation == Operations.UPDATE) {
                                val cluster = photoDao.getClusterById(localId)
                                if (cluster == null) {
                                    Log.d(TAG, "Cluster $localId was deleted locally. Ignoring ADD/UPDATE.")
                                    syncOperationDao.updateStatus(op.operationId, Status.COMPLETED)
                                    handledAnyLocally = true
                                    continue
                                }

                                val thumbnailGlobalPhotoId = cluster.thumbnailId?.let { thumbId ->
                                    photoDao.getPhotoById(thumbId)?.globalPhotoId
                                }

                                itemsToSend.add(
                                    SyncItemDto(
                                        operationId = op.operationId,
                                        entityType = "CLUSTER",
                                        operation = op.operation.name,
                                        localEntityId = op.entityId,
                                        cluster = ClusterSyncDto(
                                            globalClusterId = cluster.globalClusterId,
                                            location = cluster.location?.let {
                                                LocationSyncDto(
                                                    latitude = it.latitude,
                                                    longitude = it.longitude,
                                                    locationName = it.locationName
                                                )
                                            },
                                            thumbnailPhotoGlobalId = thumbnailGlobalPhotoId,
                                            photoCount = cluster.photoCount
                                        )
                                    )
                                )
                            } else if (op.operation == Operations.DELETE) {
                                val cluster = photoDao.getClusterById(localId)
                                val globalClusterId = cluster?.globalClusterId
                                if (globalClusterId.isNullOrBlank()) {
                                    Log.d(TAG, "Cluster $localId was never synced to server. Marking DELETE as completed.")
                                    syncOperationDao.updateStatus(op.operationId, Status.COMPLETED)
                                    handledAnyLocally = true
                                    continue
                                }

                                itemsToSend.add(
                                    SyncItemDto(
                                        operationId = op.operationId,
                                        entityType = "CLUSTER",
                                        operation = op.operation.name,
                                        localEntityId = op.entityId,
                                        cluster = ClusterSyncDto(
                                            globalClusterId = globalClusterId
                                        )
                                    )
                                )
                            }
                        }

                        EntityType.PHOTOS -> {
                            if (op.operation == Operations.ADD || op.operation == Operations.UPDATE) {
                                val photo = photoDao.getPhotoById(localId)
                                if (photo == null) {
                                    Log.d(TAG, "Photo $localId was deleted locally. Ignoring ADD/UPDATE.")
                                    syncOperationDao.updateStatus(op.operationId, Status.COMPLETED)
                                    handledAnyLocally = true
                                    continue
                                }

                                // 1. Ensure photo is uploaded to S3 first
                                var s3Key = photo.s3Key
                                if (s3Key.isNullOrBlank()) {
                                    try {
                                        Log.d(TAG, "Initiating S3 upload for photo localId=$localId, name=${photo.photoName}")
                                        val initResponse = photoApi.initiateUpload(
                                            authHeader,
                                            InitiateUploadRequest(name = photo.photoName)
                                        )
                                        if (!initResponse.isSuccessful || initResponse.body() == null) {
                                            Log.e(TAG, "Failed to initiate upload for photo $localId: HTTP ${initResponse.code()}")
                                            continue
                                        }

                                        val uploadInfo = initResponse.body()!!
                                        val photoUri = Uri.parse(photo.photoUri)
                                        Log.d(TAG, "Uploading photo bytes to presigned URL...")
                                        val uploaded = imageUploader.uploadPhotoToPresignedUrl(
                                            uploadInfo.presignedUrl,
                                            photoUri
                                        )
                                        if (!uploaded) {
                                            Log.e(TAG, "Failed to upload image bytes to S3 for photo $localId")
                                            continue
                                        }

                                        s3Key = uploadInfo.s3Key
                                        photoDao.updatePhotoS3Key(photo.localPhotoId, s3Key)
                                        Log.d(TAG, "S3 upload successful for photo $localId, s3Key=$s3Key")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error during S3 upload for photo $localId", e)
                                        continue
                                    }
                                }

                                // 2. Resolve cluster global ID if photo is attached to a cluster
                                val clusterGlobalId = photo.clusterId?.let { clusterLocalId ->
                                    photoDao.getClusterById(clusterLocalId)?.globalClusterId
                                }

                                itemsToSend.add(
                                    SyncItemDto(
                                        operationId = op.operationId,
                                        entityType = "PHOTOS",
                                        operation = op.operation.name,
                                        localEntityId = op.entityId,
                                        photo = PhotoSyncDto(
                                            globalPhotoId = photo.globalPhotoId,
                                            globalClusterId = clusterGlobalId,
                                            name = photo.photoName,
                                            s3Key = s3Key,
                                            takenAt = photo.takenAt,
                                            editedAt = photo.editedAt,
                                            location = photo.location?.let {
                                                LocationSyncDto(
                                                    latitude = it.latitude,
                                                    longitude = it.longitude,
                                                    locationName = it.locationName
                                                )
                                            },
                                            visibility = photo.visibility.name
                                        )
                                    )
                                )
                            } else if (op.operation == Operations.DELETE) {
                                val photo = photoDao.getPhotoById(localId)
                                val globalPhotoId = photo?.globalPhotoId
                                if (globalPhotoId.isNullOrBlank()) {
                                    Log.d(TAG, "Photo $localId was never synced to server. Marking DELETE as completed.")
                                    syncOperationDao.updateStatus(op.operationId, Status.COMPLETED)
                                    handledAnyLocally = true
                                    continue
                                }

                                itemsToSend.add(
                                    SyncItemDto(
                                        operationId = op.operationId,
                                        entityType = "PHOTOS",
                                        operation = op.operation.name,
                                        localEntityId = op.entityId,
                                        photo = PhotoSyncDto(
                                            globalPhotoId = globalPhotoId
                                        )
                                    )
                                )
                            }
                        }
                    }
                }

                if (itemsToSend.isEmpty()) {
                    if (!handledAnyLocally) {
                        Log.w(TAG, "No items in the current batch could be prepared. Will retry later.")
                        return Result.retry()
                    }
                    continue
                }

                Log.d(TAG, "Dispatching batch of ${itemsToSend.size} items to /api/sync/batch...")
                val syncResponse = syncApi.syncBatch(
                    authHeader,
                    SyncBatchRequest(
                        deviceId = deviceId,
                        operations = itemsToSend
                    )
                )

                if (!syncResponse.isSuccessful || syncResponse.body() == null) {
                    Log.e(TAG, "Batch sync HTTP failed with code: ${syncResponse.code()}")
                    return Result.retry()
                }

                val results = syncResponse.body()!!.results
                Log.d(TAG, "Received batch response with ${results.size} result items.")

                for (res in results) {
                    if (res.status == "SUCCESS") {
                        val localEntityId = res.localEntityId.toIntOrNull()
                        if (res.entityType.equals("CLUSTER", ignoreCase = true) && localEntityId != null && res.globalId != null) {
                            photoDao.updateClusterGlobalId(localEntityId, res.globalId)
                            Log.d(TAG, "Updated cluster $localEntityId with globalClusterId=${res.globalId}")
                        } else if (res.entityType.equals("PHOTOS", ignoreCase = true) && localEntityId != null && res.globalId != null) {
                            photoDao.updatePhotoGlobalId(localEntityId, res.globalId)
                            Log.d(TAG, "Updated photo $localEntityId with globalPhotoId=${res.globalId}")
                        }
                        syncOperationDao.updateStatus(res.operationId, Status.COMPLETED)
                    } else {
                        Log.w(TAG, "Server reported failure for operation ${res.operationId}: ${res.errorMessage}")
                    }
                }
            }

            // 2. Pull Phase: pull changes made by other devices from the cloud
            Log.i(TAG, "Push phase completed. Starting pull sync phase...")
            pullChangesFromCloud(authHeader, deviceId)

            Log.i(TAG, "Sync operation finished successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing sync operation: ${e.message}", e)
            Result.retry()
        }
    }

    private suspend fun pullChangesFromCloud(authHeader: String, deviceId: String) {
        val pullBatchSize = 20
        val picturesDir = (applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: applicationContext.filesDir).apply { mkdirs() }
        val defaultDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val userId = dataStoreManager.userIdFlow.first() ?: "unknown"

        while (true) {
            val currentLastOpId = dataStoreManager.lastSyncedOperationIdFlow.first()
            Log.d(TAG, "Pulling changes from cloud since operationId=$currentLastOpId with batchSize=$pullBatchSize")

            val pullResponse = try {
                syncApi.pullChanges(
                    bearerToken = authHeader,
                    lastOperationId = currentLastOpId,
                    batchSize = pullBatchSize,
                    deviceId = deviceId
                )
            } catch (e: Exception) {
                Log.e(TAG, "Network or server error during pull sync: ${e.message}", e)
                break
            }

            if (!pullResponse.isSuccessful || pullResponse.body() == null) {
                Log.e(TAG, "Pull sync request failed with HTTP ${pullResponse.code()}")
                break
            }

            val body = pullResponse.body()!!
            val operations = body.operations
            if (operations.isEmpty()) {
                Log.i(TAG, "No new changes to pull from cloud.")
                break
            }

            Log.i(TAG, "Received ${operations.size} operations from cloud to apply locally.")
            var latestProcessedOpId = currentLastOpId

            for (op in operations) {
                try {
                    when (op.entityType.uppercase()) {
                        "PHOTOS" -> {
                            when (op.operation.uppercase()) {
                                "ADD" -> {
                                    handlePullAddPhoto(op, picturesDir, defaultDateFormatter, userId)
                                }
                                "DELETE" -> {
                                    handlePullDeletePhoto(op)
                                }
                                "UPDATE" -> {
                                    handlePullUpdatePhoto(op)
                                }
                            }
                        }
                        "CLUSTER" -> {
                            when (op.operation.uppercase()) {
                                "ADD", "UPDATE" -> {
                                    handlePullClusterAddOrUpdate(op)
                                }
                                "DELETE" -> {
                                    handlePullClusterDelete(op)
                                }
                            }
                        }
                    }
                    latestProcessedOpId = maxOf(latestProcessedOpId, op.operationId)
                    dataStoreManager.saveLastSyncedOperationId(latestProcessedOpId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error applying pulled operation ${op.operationId}: ${e.message}", e)
                }
            }

            // If fewer items were returned than requested and hasMore is false, we are caught up
            if (!body.hasMore && operations.size < pullBatchSize) {
                break
            }
        }
    }

    private suspend fun handlePullAddPhoto(
        op: SyncPullItemDto,
        picturesDir: File,
        defaultDateFormatter: SimpleDateFormat,
        userId: String
    ) {
        val globalPhotoId = op.entityId
        // Check if photo already exists locally
        val existing = photoDao.getPhotoByGlobalPhotoId(globalPhotoId)
        if (existing != null) {
            Log.d(TAG, "Photo with globalPhotoId=$globalPhotoId already exists locally. Skipping add.")
            return
        }

        // Prepare destination file in private storage
        val safeName = (op.photo?.name ?: "photo").replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val fileName = "PinPic-Pull-${globalPhotoId.take(8)}-$safeName.jpg"
        val destFile = File(picturesDir, fileName)

        // Download the photo if downloadUrl is available
        val downloadUrl = op.downloadUrl
        if (!downloadUrl.isNullOrBlank()) {
            val downloaded = imageDownloader.downloadPhotoToFile(downloadUrl, destFile)
            if (!downloaded) {
                Log.w(TAG, "Failed to download photo file for $globalPhotoId, proceeding to save record.")
            }
        } else {
            Log.w(TAG, "No downloadUrl provided for photo $globalPhotoId")
        }

        // Resolve cluster if provided
        var clusterLocalId: Int? = null
        val globalClusterId = op.photo?.globalClusterId
        if (!globalClusterId.isNullOrBlank()) {
            val localCluster = photoDao.getClusterByGlobalClusterId(globalClusterId)
            if (localCluster != null) {
                clusterLocalId = localCluster.localClusterId
            } else {
                val newCluster = ClusterEntity(
                    globalClusterId = globalClusterId,
                    location = op.photo.location?.let {
                        com.sunflower.pinpic.data.local.entity.Location(
                            latitude = it.latitude ?: 0.0,
                            longitude = it.longitude ?: 0.0,
                            locationName = it.locationName ?: "Unknown Location",
                            isEdited = false
                        )
                    },
                    photoCount = 1
                )
                clusterLocalId = photoDao.insertCluster(newCluster).toInt()
            }
        }

        val currentDateStr = defaultDateFormatter.format(Date())
        val photoEntity = PhotoEntity(
            globalPhotoId = globalPhotoId,
            photoName = op.photo?.name ?: "Untitled",
            photoUri = destFile.absolutePath,
            s3Key = op.photo?.s3Key,
            takenAt = op.photo?.takenAt ?: currentDateStr,
            editedAt = op.photo?.editedAt ?: currentDateStr,
            location = op.photo?.location?.let {
                com.sunflower.pinpic.data.local.entity.Location(
                    latitude = it.latitude ?: 0.0,
                    longitude = it.longitude ?: 0.0,
                    locationName = it.locationName ?: "Unknown Location",
                    isEdited = false
                )
            },
            visibility = op.photo?.visibility?.let {
                try {
                    com.sunflower.pinpic.data.local.entity.Visibility.valueOf(it)
                } catch (_: Exception) {
                    com.sunflower.pinpic.data.local.entity.Visibility.PRIVATE
                }
            } ?: com.sunflower.pinpic.data.local.entity.Visibility.PRIVATE,
            userId = userId,
            clusterId = clusterLocalId
        )

        val insertedLocalId = photoDao.insertPhoto(photoEntity).toInt()
        Log.i(TAG, "Inserted pulled photo $globalPhotoId as localPhotoId=$insertedLocalId")

        // Generate thumbnail if file exists
        if (destFile.exists()) {
            try {
                ImageUtils.generateAndSaveThumbnail(
                    context = applicationContext,
                    originalUriString = destFile.absolutePath,
                    photoId = insertedLocalId.toLong()
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to generate thumbnail for pulled photo $insertedLocalId", e)
            }
        }
    }

    private suspend fun handlePullDeletePhoto(op: SyncPullItemDto) {
        val globalPhotoId = op.entityId
        val photo = photoDao.getPhotoByGlobalPhotoId(globalPhotoId)
        if (photo == null) {
            Log.d(TAG, "Photo with globalPhotoId=$globalPhotoId not found locally. Nothing to delete.")
            return
        }

        // 1. Delete photo from private storage
        try {
            val file = File(photo.photoUri)
            if (file.exists()) {
                val deleted = file.delete()
                Log.d(TAG, "Deleted local file ${photo.photoUri}: $deleted")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete local photo file ${photo.photoUri}", e)
        }

        // Delete thumbnail file if present
        try {
            photo.lowerResolutionPhotoUri?.let { thumbPath ->
                val thumbFile = File(thumbPath)
                if (thumbFile.exists()) thumbFile.delete()
            }
        } catch (_: Exception) {}

        // 2. Delete photo from database and cleanup clusters
        photoDao.deletePhotosAndCleanupClusters(listOf(photo))
        Log.i(TAG, "Deleted pulled photo $globalPhotoId from local database")
    }

    private suspend fun handlePullUpdatePhoto(op: SyncPullItemDto) {
        val globalPhotoId = op.entityId
        val photo = photoDao.getPhotoByGlobalPhotoId(globalPhotoId) ?: return
        val updated = photo.copy(
            photoName = op.photo?.name ?: photo.photoName,
            editedAt = op.photo?.editedAt ?: photo.editedAt,
            location = op.photo?.location?.let {
                com.sunflower.pinpic.data.local.entity.Location(
                    latitude = it.latitude ?: 0.0,
                    longitude = it.longitude ?: 0.0,
                    locationName = it.locationName ?: "Unknown Location",
                    isEdited = false
                )
            } ?: photo.location,
            visibility = op.photo?.visibility?.let {
                try {
                    com.sunflower.pinpic.data.local.entity.Visibility.valueOf(it)
                } catch (_: Exception) {
                    photo.visibility
                }
            } ?: photo.visibility
        )
        photoDao.updatePhoto(updated)
    }

    private suspend fun handlePullClusterAddOrUpdate(op: SyncPullItemDto) {
        val globalClusterId = op.entityId
        val existing = photoDao.getClusterByGlobalClusterId(globalClusterId)
        val loc = op.cluster?.location?.let {
            com.sunflower.pinpic.data.local.entity.Location(
                latitude = it.latitude ?: 0.0,
                longitude = it.longitude ?: 0.0,
                locationName = it.locationName ?: "Unknown Location",
                isEdited = false
            )
        }
        if (existing != null) {
            val updated = existing.copy(
                location = loc ?: existing.location,
                photoCount = op.cluster?.photoCount ?: existing.photoCount
            )
            photoDao.updateCluster(updated)
        } else {
            val newCluster = ClusterEntity(
                globalClusterId = globalClusterId,
                location = loc,
                photoCount = op.cluster?.photoCount ?: 0
            )
            photoDao.insertCluster(newCluster)
        }
    }

    private suspend fun handlePullClusterDelete(op: SyncPullItemDto) {
        val globalClusterId = op.entityId
        val existing = photoDao.getClusterByGlobalClusterId(globalClusterId) ?: return
        photoDao.deleteClusters(listOf(existing))
    }

    companion object {
        const val TAG = "SyncWorker"
        const val UNIQUE_WORK_NAME = "PinPicSyncWork"
        private const val NOTIFICATION_ID = 1001
    }
}
