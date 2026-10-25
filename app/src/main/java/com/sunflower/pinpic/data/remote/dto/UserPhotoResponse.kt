package com.sunflower.pinpic.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserPhotoResponse(
    val photoId: String,
    val userId: String,
    val name: String? = null,
    val downloadUrl: String? = null,
    val visibility: String? = null,
    val takenAt: String? = null
)

