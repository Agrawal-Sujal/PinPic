package com.sunflower.pinpic.domain.usecase

import com.sunflower.pinpic.domain.model.Photo
import com.sunflower.pinpic.domain.repository.PhotosRepository
import com.sunflower.pinpic.common.Result
import com.sunflower.pinpic.domain.model.ClusterPoint
import javax.inject.Inject

class SavePhotosUseCase @Inject constructor(
    private val repository: PhotosRepository
) {
    suspend operator fun invoke(photos: List<Photo>,latitude: Double,longitude: Double): Result<Unit> {
        val cityName = "Unknown Location"

        val updatedPhotos = photos.map { photo ->
            if (photo.locationName == null) {
                photo.copy(locationName = cityName)
            } else {
                photo
            }
        }

        val cluster = ClusterPoint(
            lat = latitude,
            lng = longitude,
            count = updatedPhotos.size
        )
        return repository.saveClusterWithPhotos(cluster, updatedPhotos)
    }
}
