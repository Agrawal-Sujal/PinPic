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
import com.sunflower.pinpic.data.remote.dto.SyncPushRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            val batchSize = 20
            while (true) {
                val pendingBatch = syncOperationDao.getPendingOperationsBatch(batchSize)
                if (pendingBatch.isEmpty()) {
                    Log.i(TAG, "All pending push operations have been processed.")
                    break
                }
                Log.d(TAG, "Fetched batch of ${pendingBatch.size} pending operations.")

                val clustersToSend = mutableListOf<ClusterSyncDto>()
                val photosToSend = mutableListOf<PhotoSyncDto>()
                val processedOps = mutableListOf<com.sunflower.pinpic.data.local.entity.SyncOperationEntity>()
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
                                if (cluster == null || cluster.isDeleted) {
                                    Log.d(TAG, "Cluster $localId was deleted locally. Ignoring ADD/UPDATE.")
                                    syncOperationDao.updateStatus(op.operationId, Status.COMPLETED)
                                    handledAnyLocally = true
                                    continue
                                }

                                var globalClusterId = cluster.globalClusterId
                                if (globalClusterId.isNullOrBlank()) {
                                    globalClusterId = java.util.UUID.randomUUID().toString()
                                    photoDao.updateClusterGlobalId(localId, globalClusterId)
                                }

                                val thumbnailGlobalPhotoId = cluster.thumbnailId?.let { thumbId ->
                                    photoDao.getPhotoById(thumbId)?.globalPhotoId
                                }

                                clustersToSend.add(
                                    ClusterSyncDto(
                                        globalClusterId = globalClusterId,
                                        location = cluster.location?.let {
                                            LocationSyncDto(
                                                latitude = it.latitude,
                                                longitude = it.longitude,
                                                locationName = it.locationName
                                            )
                                        },
                                        thumbnailPhotoGlobalId = thumbnailGlobalPhotoId,
                                        photoCount = cluster.photoCount,
                                        visibility = cluster.visibility.name,
                                        publicPhotoCount = cluster.publicPhotoCount,
                                        privatePhotoCount = cluster.privatePhotoCount,
                                        friendPhotoCount = cluster.friendPhotoCount,
                                        isDeleted = false
                                    )
                                )
                                processedOps.add(op)
                            } else if (op.operation == Operations.DELETE) {
                                val cluster = photoDao.getClusterById(localId)
                                val globalClusterId = cluster?.globalClusterId
                                if (globalClusterId.isNullOrBlank()) {
                                    Log.d(TAG, "Cluster $localId was never synced to server. Marking DELETE as completed.")
                                    photoDao.purgeClusterById(localId)
                                    syncOperationDao.updateStatus(op.operationId, Status.COMPLETED)
                                    handledAnyLocally = true
                                    continue
                                }

                                clustersToSend.add(
                                    ClusterSyncDto(
                                        globalClusterId = globalClusterId,
                                        isDeleted = true
                                    )
                                )
                                processedOps.add(op)
                            }
                        }

                        EntityType.PHOTOS -> {
                            if (op.operation == Operations.ADD || op.operation == Operations.UPDATE) {
                                val photo = photoDao.getPhotoById(localId)
                                if (photo == null || photo.isDeleted) {
                                    Log.d(TAG, "Photo $localId was deleted locally. Ignoring ADD/UPDATE.")
                                    syncOperationDao.updateStatus(op.operationId, Status.COMPLETED)
                                    handledAnyLocally = true
                                    continue
                                }

                                var globalPhotoId = photo.globalPhotoId
                                if (globalPhotoId.isNullOrBlank()) {
                                    globalPhotoId = java.util.UUID.randomUUID().toString()
                                    photoDao.updatePhotoGlobalId(localId, globalPhotoId)
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

                                photosToSend.add(
                                    PhotoSyncDto(
                                        globalPhotoId = globalPhotoId,
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
                                        visibility = photo.visibility.name,
                                        isDeleted = false
                                    )
                                )
                                processedOps.add(op)
                            } else if (op.operation == Operations.DELETE) {
                                val photo = photoDao.getPhotoById(localId)
                                val globalPhotoId = photo?.globalPhotoId
                                if (globalPhotoId.isNullOrBlank()) {
                                    Log.d(TAG, "Photo $localId was never synced to server. Marking DELETE as completed.")
                                    photo?.photoUri?.let { uriStr ->
                                        try {
                                            val f = File(uriStr)
                                            if (f.exists()) f.delete()
                                        } catch (_: Exception) {}
                                    }
                                    photo?.lowerResolutionPhotoUri?.let { thumbStr ->
                                        try {
                                            val f = File(thumbStr)
                                            if (f.exists()) f.delete()
                                        } catch (_: Exception) {}
                                    }
                                    photoDao.purgePhotoById(localId)
                                    syncOperationDao.updateStatus(op.operationId, Status.COMPLETED)
                                    handledAnyLocally = true
                                    continue
                                }

                                photosToSend.add(
                                    PhotoSyncDto(
                                        globalPhotoId = globalPhotoId,
                                        isDeleted = true
                                    )
                                )
                                processedOps.add(op)
                            }
                        }
                    }
                }

                if (clustersToSend.isEmpty() && photosToSend.isEmpty()) {
                    if (!handledAnyLocally) {
                        Log.w(TAG, "No items in the current batch could be prepared. Will retry later.")
                        return Result.retry()
                    }
                    continue
                }

                Log.d(TAG, "Dispatching ${clustersToSend.size} clusters and ${photosToSend.size} photos to /api/sync/push...")
                val pushResponse = syncApi.pushMutations(
                    authHeader,
                    SyncPushRequest(
                        deviceId = deviceId,
                        clusters = clustersToSend,
                        photos = photosToSend
                    )
                )

                if (!pushResponse.isSuccessful || pushResponse.body() == null) {
                    Log.e(TAG, "Push mutations HTTP failed with code: ${pushResponse.code()}")
                    return Result.retry()
                }

                Log.d(TAG, "Batch mutations pushed successfully.")
                for (op in processedOps) {
                    val localEntityId = op.entityId.toIntOrNull()
                    if (op.operation == Operations.DELETE && localEntityId != null) {
                        if (op.entityType == EntityType.PHOTOS) {
                            val photo = photoDao.getPhotoById(localEntityId)
                            photo?.photoUri?.let { uriStr ->
                                try {
                                    val f = File(uriStr)
                                    if (f.exists()) f.delete()
                                } catch (_: Exception) {}
                            }
                            photo?.lowerResolutionPhotoUri?.let { thumbStr ->
                                try {
                                    val f = File(thumbStr)
                                    if (f.exists()) f.delete()
                                } catch (_: Exception) {}
                            }
                            photoDao.purgePhotoById(localEntityId)
                            Log.d(TAG, "Purged soft-deleted photo $localEntityId after successful push")
                        } else if (op.entityType == EntityType.CLUSTER) {
                            photoDao.purgeClusterById(localEntityId)
                            Log.d(TAG, "Purged soft-deleted cluster $localEntityId after successful push")
                        }
                    }
                    syncOperationDao.updateStatus(op.operationId, Status.COMPLETED)
                }
            }

            // 2. Pull Phase: pull changes made by other devices from the cloud
            Log.i(TAG, "Push phase completed. Starting pull sync phase...")
            pullDeltasFromCloud(authHeader, deviceId)

            Log.i(TAG, "Sync operation finished successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing sync operation: ${e.message}", e)
            Result.retry()
        }
    }

    private suspend fun pullDeltasFromCloud(authHeader: String, deviceId: String) {
        val pullBatchSize = 100
        val picturesDir = (applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: applicationContext.filesDir).apply { mkdirs() }
        val defaultDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val userId = dataStoreManager.userIdFlow.first() ?: "unknown"

        while (true) {
            val currentCursor = dataStoreManager.lastSyncedRevisionFlow.first()
            Log.d(TAG, "Pulling deltas from cloud since revision=$currentCursor with batchSize=$pullBatchSize")

            val pullResponse = try {
                syncApi.pullDeltas(
                    bearerToken = authHeader,
                    cursor = currentCursor,
                    limit = pullBatchSize,
                    deviceId = deviceId
                )
            } catch (e: Exception) {
                Log.e(TAG, "Delta pull request failed: ${e.message}", e)
                break
            }

            if (!pullResponse.isSuccessful || pullResponse.body() == null) {
                Log.e(TAG, "Delta pull returned HTTP ${pullResponse.code()}")
                break
            }

            val body = pullResponse.body()!!
            if (body.resetRequired) {
                Log.w(TAG, "Server requested sync reset. Resetting local cursor...")
                dataStoreManager.saveLastSyncedRevision(0L)
                return
            }

            val clusters = body.clusters
            val photos = body.photos

            if (clusters.isEmpty() && photos.isEmpty()) {
                Log.i(TAG, "No new deltas from cloud.")
                break
            }

            Log.i(TAG, "Applying ${clusters.size} cluster deltas and ${photos.size} photo deltas.")

            // 1. Process Clusters
            for (c in clusters) {
                try {
                    val globalClusterId = c.clusterId
                    val existing = photoDao.getClusterByGlobalClusterId(globalClusterId)

                    if (c.isDeleted) {
                        if (existing != null) {
                            photoDao.purgeClusterById(existing.localClusterId)
                        }
                    } else if (c.isEcho) {
                        // ECHO: only advance revision
                        photoDao.updateClusterRevision(globalClusterId, c.revision)
                    } else {
                        // External change: upsert
                        val clusterData = ClusterEntity(
                            localClusterId = existing?.localClusterId ?: 0,
                            globalClusterId = globalClusterId,
                            location = c.location?.let {
                                com.sunflower.pinpic.data.local.entity.Location(
                                    latitude = it.latitude ?: 0.0,
                                    longitude = it.longitude ?: 0.0,
                                    locationName = it.locationName ?: "Unknown Location",
                                    isEdited = false
                                )
                            },
                            photoCount = c.photoCount ?: 0,
                            visibility = c.visibility?.let {
                                try { com.sunflower.pinpic.data.local.entity.Visibility.valueOf(it) }
                                catch (_: Exception) { com.sunflower.pinpic.data.local.entity.Visibility.PRIVATE }
                            } ?: com.sunflower.pinpic.data.local.entity.Visibility.PRIVATE,
                            publicPhotoCount = c.publicPhotoCount ?: 0,
                            privatePhotoCount = c.privatePhotoCount ?: 0,
                            friendPhotoCount = c.friendPhotoCount ?: 0,
                            revision = c.revision,
                            isDeleted = false
                        )
                        if (existing != null) {
                            photoDao.updateClusters(listOf(clusterData))
                        } else {
                            photoDao.insertCluster(clusterData)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error applying cluster delta ${c.clusterId}: ${e.message}", e)
                }
            }

            // 2. Process Photos
            for (p in photos) {
                try {
                    val globalPhotoId = p.photoId
                    val existing = photoDao.getPhotoByGlobalPhotoId(globalPhotoId)

                    if (p.isDeleted) {
                        if (existing != null) {
                            existing.photoUri.let { uriStr ->
                                try { val f = File(uriStr); if (f.exists()) f.delete() } catch (_: Exception) {}
                            }
                            existing.lowerResolutionPhotoUri?.let { thumbStr ->
                                try { val f = File(thumbStr); if (f.exists()) f.delete() } catch (_: Exception) {}
                            }
                            photoDao.purgePhotoById(existing.localPhotoId)
                        }
                    } else if (p.isEcho) {
                        // ECHO: only update revision, NEVER overwrite local file paths!
                        photoDao.updatePhotoRevision(globalPhotoId, p.revision)
                    } else {
                        // External change: resolve cluster
                        var clusterLocalId: Int? = null
                        p.clusterId?.let { cId ->
                            clusterLocalId = photoDao.getClusterByGlobalClusterId(cId)?.localClusterId
                        }

                        if (existing != null) {
                            // Update existing record, PRESERVE local photoUri!
                            val updated = existing.copy(
                                photoName = p.name ?: existing.photoName,
                                clusterId = clusterLocalId ?: existing.clusterId,
                                s3Key = p.s3Key ?: existing.s3Key,
                                location = p.location?.let {
                                    com.sunflower.pinpic.data.local.entity.Location(
                                        latitude = it.latitude ?: 0.0,
                                        longitude = it.longitude ?: 0.0,
                                        locationName = it.locationName ?: "Unknown Location",
                                        isEdited = false
                                    )
                                } ?: existing.location,
                                visibility = p.visibility?.let {
                                    try { com.sunflower.pinpic.data.local.entity.Visibility.valueOf(it) }
                                    catch (_: Exception) { existing.visibility }
                                } ?: existing.visibility,
                                revision = p.revision
                            )
                            photoDao.updatePhoto(updated)
                        } else {
                            // New photo from another device: download image binary
                            val safeName = (p.name ?: "photo").replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                            val fileName = "PinPic-Pull-${globalPhotoId.take(8)}-$safeName.jpg"
                            val destFile = File(picturesDir, fileName)

                            p.downloadUrl?.let { url ->
                                imageDownloader.downloadPhotoToFile(url, destFile)
                            }

                            val currentDateStr = defaultDateFormatter.format(Date())
                            val newPhoto = PhotoEntity(
                                globalPhotoId = globalPhotoId,
                                photoName = p.name ?: "Untitled",
                                photoUri = destFile.absolutePath,
                                s3Key = p.s3Key,
                                takenAt = p.takenAt ?: currentDateStr,
                                editedAt = p.editedAt ?: currentDateStr,
                                location = p.location?.let {
                                    com.sunflower.pinpic.data.local.entity.Location(
                                        latitude = it.latitude ?: 0.0,
                                        longitude = it.longitude ?: 0.0,
                                        locationName = it.locationName ?: "Unknown Location",
                                        isEdited = false
                                    )
                                },
                                visibility = p.visibility?.let {
                                    try { com.sunflower.pinpic.data.local.entity.Visibility.valueOf(it) }
                                    catch (_: Exception) { com.sunflower.pinpic.data.local.entity.Visibility.PRIVATE }
                                } ?: com.sunflower.pinpic.data.local.entity.Visibility.PRIVATE,
                                userId = userId,
                                clusterId = clusterLocalId,
                                revision = p.revision
                            )

                            val insertedId = photoDao.insertPhoto(newPhoto).toInt()
                            if (destFile.exists()) {
                                try {
                                    ImageUtils.generateAndSaveThumbnail(
                                        context = applicationContext,
                                        originalUriString = destFile.absolutePath,
                                        photoId = insertedId.toLong()
                                    )
                                } catch (_: Exception) {}
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error applying photo delta ${p.photoId}: ${e.message}", e)
                }
            }

            dataStoreManager.saveLastSyncedRevision(body.nextCursor)

            if (!body.hasMore) {
                break
            }
        }
    }

    companion object {
        const val TAG = "SyncWorker"
        const val UNIQUE_WORK_NAME = "PinPicSyncWork"
        private const val NOTIFICATION_ID = 1001
    }
}
