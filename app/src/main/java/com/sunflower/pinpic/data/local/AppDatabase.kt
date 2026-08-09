package com.sunflower.pinpic.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sunflower.pinpic.data.local.dao.PhotoDao
import com.sunflower.pinpic.data.local.entity.ClusterPointEntity
import com.sunflower.pinpic.data.local.entity.PhotoEntity
import com.sunflower.pinpic.data.local.entity.LocationInfoEntity
import com.sunflower.pinpic.data.local.dao.LocationInfoDao

@Database(entities = [PhotoEntity::class, ClusterPointEntity::class, LocationInfoEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
    abstract fun locationInfoDao(): LocationInfoDao
}
