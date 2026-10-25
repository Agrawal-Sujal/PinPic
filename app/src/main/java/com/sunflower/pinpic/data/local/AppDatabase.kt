package com.sunflower.pinpic.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sunflower.pinpic.data.local.dao.PhotoDao
import com.sunflower.pinpic.data.local.dao.SyncOperationDao
import com.sunflower.pinpic.data.local.entity.ClusterEntity
import com.sunflower.pinpic.data.local.entity.PhotoEntity
import com.sunflower.pinpic.data.local.entity.SyncOperationEntity

@Database(
    entities = [
        PhotoEntity::class,
        ClusterEntity::class,
        SyncOperationEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
    abstract fun syncOperationDao(): SyncOperationDao
}

