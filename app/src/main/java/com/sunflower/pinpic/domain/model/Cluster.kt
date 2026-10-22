package com.sunflower.pinpic.domain.model

data class Cluster(
    val localClusterId: Int = 0,
    val globalClusterId: Int? = null,
    val photos: List<Photo>? = null,
    val location: Location? = null,
    val thumbnail: Photo? = null,
    val photoCount: Int = 0
)
