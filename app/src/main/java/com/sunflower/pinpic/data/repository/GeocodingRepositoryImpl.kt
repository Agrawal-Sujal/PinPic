package com.sunflower.pinpic.data.repository

import com.sunflower.pinpic.BuildConfig
import com.sunflower.pinpic.data.local.dao.LocationInfoDao
import com.sunflower.pinpic.data.local.entity.LocationInfoEntity
import com.sunflower.pinpic.data.remote.api.GeoapifyApi
import com.sunflower.pinpic.data.remote.dto.GeoapifyProperties
import com.sunflower.pinpic.domain.model.LocationInfo
import com.sunflower.pinpic.domain.repository.GeocodingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.math.roundToInt

class GeocodingRepositoryImpl @Inject constructor(
    private val geoapifyApi: GeoapifyApi,
    private val locationInfoDao: LocationInfoDao,
    private val json: Json
) : GeocodingRepository {

    override suspend fun getLocationInfo(lat: Double, lon: Double): Result<LocationInfo> = withContext(Dispatchers.IO) {
        try {
            // Round to 4 decimal places for caching (~11 meters)
            val rLat = (lat * 10000.0).roundToInt() / 10000.0
            val rLon = (lon * 10000.0).roundToInt() / 10000.0
            val cacheKey = "${rLat},${rLon}"

            val cached = locationInfoDao.getLocationInfo(cacheKey)
            if (cached != null) {
                val properties = json.decodeFromString<GeoapifyProperties>(cached.propertiesJson)
                return@withContext Result.success(properties.toDomain())
            }

            val apiKey = BuildConfig.GEOAPIFY_API_KEY
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("Geoapify API key is missing."))
            }

            val response = geoapifyApi.getReverseGeocode(lat = lat, lon = lon, apiKey = apiKey)
            val feature = response.features.firstOrNull()
            
            if (feature?.properties != null) {
                val propertiesJson = json.encodeToString(feature.properties)
                locationInfoDao.insertLocationInfo(
                    LocationInfoEntity(id = cacheKey, propertiesJson = propertiesJson)
                )
                Result.success(feature.properties.toDomain())
            } else {
                Result.failure(Exception("No address found for these coordinates."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun GeoapifyProperties.toDomain(): LocationInfo {
        return LocationInfo(
            name = name,
            country = country,
            state = state,
            city = city,
            street = street,
            formatted = formatted
        )
    }
}
