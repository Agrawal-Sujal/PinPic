package com.sunflower.pinpic.domain.usecase

import com.sunflower.pinpic.domain.model.LocationInfo
import com.sunflower.pinpic.domain.repository.GeocodingRepository
import javax.inject.Inject

class GetLocationInfoUseCase @Inject constructor(
    private val repository: GeocodingRepository
) {
    suspend operator fun invoke(lat: Double, lon: Double): Result<LocationInfo> {
        return repository.getLocationInfo(lat, lon)
    }
}
