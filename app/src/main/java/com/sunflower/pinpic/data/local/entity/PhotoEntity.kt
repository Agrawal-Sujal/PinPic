package com.sunflower.pinpic.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val localPhotoId: Int = 0,
    val globalPhotoId: String? = null,
    val userId: String,
    val photoName: String,
    val s3Key: String? = null,
    val takenAt: String,
    val uploadedAt: String? = null,
    val editedAt: String,
    @Embedded
    val location: Location? = null,
    val visibility: Visibility = Visibility.PRIVATE,
    val photoUri: String,
    val lowerResolutionPhotoUri: String? = null
)
