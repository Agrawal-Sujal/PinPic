package com.sunflower.pinpic.domain.model

data class Location(
    val latitude: Double,
    val longitude: Double,
    val locationName: String?,
    val isEdited: Boolean = false
)
