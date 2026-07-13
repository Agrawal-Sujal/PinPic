package com.sunflower.pinpic.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val lat: Double?,
    val lng: Double?,
    val date: String?,
    val time: String?,
    val locationName: String?,
    val uri: String?,
    val url: String?
)
