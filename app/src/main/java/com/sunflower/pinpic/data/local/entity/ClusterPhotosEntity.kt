package com.sunflower.pinpic.data.local.entity

import androidx.room.Entity

@Entity(tableName = "cluster_photos", primaryKeys = ["photoId", "clusterId"])
data class ClusterPhotosEntity(
    val photoId: Int,
    val clusterId: Int
)
