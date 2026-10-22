package com.sunflower.pinpic.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class InitiateUploadRequest(
    val name: String
)

@Serializable
data class InitiateUploadResponse(
    val s3Key: String,
    val presignedUrl: String
)

@Serializable
data class ConfirmUploadRequest(
    val s3Key: String
)

@Serializable
data class ConfirmUploadResponse(
    val photoUrl: String
)

@Serializable
data class UpdateProfileRequest(
    val name: String,
    val mobileNumber: String,
    val locale: String,
    val pictureUrl: String
)
