package com.sunflower.pinpic.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clusters")
data class ClusterEntity(
    @PrimaryKey(autoGenerate = true)
    val localClusterId: Int = 0,
    val globalClusterId: String? = null,
    @Embedded
    val location: Location? = null,
    val thumbnailId: Int? = null,
    val photoCount: Int = 0
)
