package com.sunflower.pinpic.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LocationSyncDto(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null
)

@Serializable
data class ClusterSyncDto(
    val globalClusterId: String? = null,
    val location: LocationSyncDto? = null,
    val thumbnailPhotoGlobalId: String? = null,
    val photoCount: Int? = null
)

@Serializable
data class PhotoSyncDto(
    val globalPhotoId: String? = null,
    val globalClusterId: String? = null,
    val name: String? = null,
    val s3Key: String? = null,
    val takenAt: String? = null,
    val editedAt: String? = null,
    val location: LocationSyncDto? = null,
    val visibility: String? = null,
    val size: Long? = null
)

@Serializable
data class SyncItemDto(
    val operationId: String,
    val entityType: String,
    val operation: String,
    val localEntityId: String,
    val cluster: ClusterSyncDto? = null,
    val photo: PhotoSyncDto? = null
)

@Serializable
data class SyncBatchRequest(
    val deviceId: String,
    val operations: List<SyncItemDto>
)

@Serializable
data class SyncResultDto(
    val operationId: String,
    val localEntityId: String,
    val entityType: String,
    val globalId: String? = null,
    val status: String,
    val errorMessage: String? = null
)

@Serializable
data class SyncBatchResponse(
    val results: List<SyncResultDto>
)

@Serializable
data class SyncPullRequest(
    val deviceId: String? = null,
    val lastOperationId: Long? = 0L,
    val batchSize: Int? = 50
)

@Serializable
data class SyncPullItemDto(
    val operationId: Long,
    val entityType: String,
    val operation: String,
    val entityId: String,
    val deviceId: String? = null,
    val photo: PhotoSyncDto? = null,
    val cluster: ClusterSyncDto? = null,
    val downloadUrl: String? = null
)

@Serializable
data class SyncPullResponse(
    val operations: List<SyncPullItemDto> = emptyList(),
    val lastOperationId: Long? = null,
    val hasMore: Boolean = false
)

