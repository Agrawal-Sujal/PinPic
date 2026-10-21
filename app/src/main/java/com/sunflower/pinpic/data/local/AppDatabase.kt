package com.sunflower.pinpic.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sunflower.pinpic.data.local.dao.PhotoDao
import com.sunflower.pinpic.data.local.entity.ClusterEntity
import com.sunflower.pinpic.data.local.entity.PhotoEntity

@Database(entities = [PhotoEntity::class, ClusterEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
}
