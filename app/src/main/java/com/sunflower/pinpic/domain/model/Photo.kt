package com.sunflower.pinpic.domain.model

data class Photo(
    val localPhotoId: Int = 0,
    val globalPhotoId: String? = null,
    val clusterId: Int? = null,
    val userId: String,
    val photoName: String,
    val takenAt: String,
    val uploadedAt: String? = null,
    val editedAt: String,
    val location: Location? = null,
    val photoUrl: String,
    val lowerResolutionPhotoUrl: String? = null
)
