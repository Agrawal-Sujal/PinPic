package com.sunflower.pinpic.domain.usecase

import com.sunflower.pinpic.domain.model.Photo
import com.sunflower.pinpic.domain.repository.PhotoRepository
import com.sunflower.pinpic.common.Result
import com.sunflower.pinpic.domain.model.Cluster
import com.sunflower.pinpic.domain.model.Location
import javax.inject.Inject

class SavePhotosUseCase @Inject constructor(
    private val repository: PhotoRepository
) {
    suspend operator fun invoke(photos: List<Photo>,latitude: Double,longitude: Double): Result<Unit> {
        val cityName = "Unknown Location"

        val updatedPhotos = photos.map { photo ->
            if (photo.location?.locationName == null) {
                photo.copy(location = photo.location?.copy(locationName = cityName) ?: Location(latitude, longitude, cityName))
            } else {
                photo
            }
        }

        val emptyCluster = Cluster(
            location = Location(latitude, longitude, cityName)
        )
        val clusterId = repository.saveCluster(emptyCluster)
        val updatedCluster = emptyCluster.copy(
            localClusterId = clusterId,
            photoCount = updatedPhotos.size,
            thumbnail = updatedPhotos.firstOrNull(),
            photos = updatedPhotos
        )
        repository.saveCluster(updatedCluster)
        repository.addPhotosInCluster(updatedPhotos, updatedCluster)
        return Result.Success(Unit)
    }
}
