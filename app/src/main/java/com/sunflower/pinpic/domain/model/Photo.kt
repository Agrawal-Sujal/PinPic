package com.sunflower.pinpic.domain.model

import com.sunflower.pinpic.data.local.entity.Visibility

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
    val visibility: Visibility = Visibility.PRIVATE,
    val photoUrl: String,
    val lowerResolutionPhotoUrl: String? = null
)
