package com.sunflower.pinpic.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sunflower.pinpic.data.local.entity.LocationInfoEntity

@Dao
interface LocationInfoDao {
    @Query("SELECT * FROM location_info WHERE id = :id")
    suspend fun getLocationInfo(id: String): LocationInfoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationInfo(locationInfo: LocationInfoEntity)
}
