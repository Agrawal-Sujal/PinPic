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
    val photoCount: Int? = null,
    val visibility: String? = null,
    val publicPhotoCount: Int? = null,
    val privatePhotoCount: Int? = null,
    val friendPhotoCount: Int? = null,
    val isDeleted: Boolean? = false,
    val revision: Long? = null
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
    val size: Long? = null,
    val isDeleted: Boolean? = false,
    val revision: Long? = null
)

@Serializable
data class PhotoDeltaDto(
    val photoId: String,
    val revision: Long = 0L,
    val isEcho: Boolean = false,
    val isDeleted: Boolean = false,
    val clusterId: String? = null,
    val name: String? = null,
    val s3Key: String? = null,
    val downloadUrl: String? = null,
    val takenAt: String? = null,
    val editedAt: String? = null,
    val location: LocationSyncDto? = null,
    val visibility: String? = null,
    val size: Long? = null
)

@Serializable
data class ClusterDeltaDto(
    val clusterId: String,
    val revision: Long = 0L,
    val isEcho: Boolean = false,
    val isDeleted: Boolean = false,
    val location: LocationSyncDto? = null,
    val thumbnailPhotoId: String? = null,
    val photoCount: Int? = null,
    val visibility: String? = null,
    val publicPhotoCount: Int? = null,
    val privatePhotoCount: Int? = null,
    val friendPhotoCount: Int? = null
)

@Serializable
data class SyncPushRequest(
    val deviceId: String,
    val clusters: List<ClusterSyncDto>? = null,
    val photos: List<PhotoSyncDto>? = null
)

@Serializable
data class SyncPushResponse(
    val status: String,
    val message: String? = null
)

@Serializable
data class DeltaSyncPullResponse(
    val clusters: List<ClusterDeltaDto> = emptyList(),
    val photos: List<PhotoDeltaDto> = emptyList(),
    val nextCursor: Long = 0L,
    val hasMore: Boolean = false,
    val resetRequired: Boolean = false
)

