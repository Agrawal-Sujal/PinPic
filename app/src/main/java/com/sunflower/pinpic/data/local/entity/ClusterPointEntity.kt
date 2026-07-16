package com.sunflower.pinpic.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cluster_points")
data class ClusterPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val lat: Double,
    val lng: Double,
    val count: Int,
    val thumbnailUri: String? = null
)
