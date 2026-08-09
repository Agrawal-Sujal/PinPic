package com.sunflower.pinpic.domain.repository

import com.sunflower.pinpic.domain.model.LocationInfo

interface GeocodingRepository {
    suspend fun getLocationInfo(lat: Double, lon: Double): Result<LocationInfo>
}
