package com.sunflower.pinpic.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sunflower.pinpic.data.local.entity.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @JvmSuppressWildcards
    suspend fun insertPhoto(photo: PhotoEntity): Long

    @Query("SELECT * FROM photos ORDER BY id DESC")
    suspend fun getAllPhotos(): List<PhotoEntity>

    @Query("SELECT * FROM photos ORDER BY id DESC")
    fun getAllPhotosFlow(): Flow<List<PhotoEntity>>
}