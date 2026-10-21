package com.sunflower.pinpic.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeviceRegistrationRequest(
    val device: String? = null,
    val deviceId: String? = null,
    val userId: String? = null,
    val lastAckCursor: String? = null
)

@Serializable
data class DeviceResponse(
    val deviceId: String? = null,
    val device: String? = null,
    val userId: String? = null,
    val lastAckCursor: String? = null,
    val lastSeenAt: String? = null,
    val createdAt: String? = null,
    val lastLogin: String? = null
)
