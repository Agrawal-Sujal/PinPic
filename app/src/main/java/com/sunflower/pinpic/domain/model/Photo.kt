package com.sunflower.pinpic.domain.model

data class Photo(
    val id: Long = 0,
    val lat: Double?,
    val lng: Double?,
    val date: String?,
    val time: String?,
    val locationName: String?,
    val uri: String?,
    val url: String?
)
