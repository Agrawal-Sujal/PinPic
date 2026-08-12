package com.sunflower.pinpic.domain.model

data class ClusterPoint(
    val id: Long = 0,
    val lat: Double,
    val lng: Double,
    val count: Int,
    val thumbnailUri: String? = null,
    val locationName: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null
)
