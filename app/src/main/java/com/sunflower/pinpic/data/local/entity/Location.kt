package com.sunflower.pinpic.data.local.entity

data class Location(
    val latitude: Double,
    val longitude: Double,
    val locationName: String?,
    val isEdited: Boolean = false
)
