package com.sunflower.pinpic.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserClusterLocation(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null
)

@Serializable
data class UserClusterResponse(
    val clusterId: String,
    val userId: String,
    val location: UserClusterLocation? = null,
    val thumbnailPhotoId: String? = null,
    val thumbnailUrl: String? = null,
    val photoCount: Int = 0,
    val visibility: String? = null,
    val photos: List<UserPhotoResponse> = emptyList()
)

