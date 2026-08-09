package com.sunflower.pinpic.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_info")
data class LocationInfoEntity(
    @PrimaryKey
    val id: String, // e.g., "lat,lon" rounded to 4 decimals
    val propertiesJson: String // Serialized GeoapifyProperties
)
