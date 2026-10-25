package com.sunflower.pinpic.data.mapper

import com.sunflower.pinpic.data.local.entity.ClusterEntity
import com.sunflower.pinpic.data.local.entity.PhotoEntity
import com.sunflower.pinpic.domain.model.Cluster
import com.sunflower.pinpic.domain.model.Photo
import com.sunflower.pinpic.data.local.entity.Location as EntityLocation
import com.sunflower.pinpic.domain.model.Location as DomainLocation

fun DomainLocation.toEntity(): EntityLocation {
    return EntityLocation(
        latitude = latitude,
        longitude = longitude,
        locationName = locationName,
        isEdited = isEdited
    )
}

fun EntityLocation.toDomain(): DomainLocation {
    return DomainLocation(
        latitude = latitude,
        longitude = longitude,
        locationName = locationName,
        isEdited = isEdited
    )
}

fun Photo.toEntity(): PhotoEntity {
    return PhotoEntity(
        localPhotoId = localPhotoId,
        globalPhotoId = globalPhotoId,
        clusterId = clusterId,
        userId = userId,
        photoName = photoName,
        takenAt = takenAt,
        uploadedAt = uploadedAt,
        editedAt = editedAt,
        location = location?.toEntity(),
        visibility = visibility,
        photoUri = photoUrl, // assuming photoUrl in domain maps to photoUri in entity
        lowerResolutionPhotoUri = lowerResolutionPhotoUrl
    )
}

fun PhotoEntity.toDomain(): Photo {
    return Photo(
        localPhotoId = localPhotoId,
        globalPhotoId = globalPhotoId,
        clusterId = clusterId,
        userId = userId,
        photoName = photoName,
        takenAt = takenAt,
        uploadedAt = uploadedAt,
        editedAt = editedAt,
        location = location?.toDomain(),
        visibility = visibility,
        photoUrl = photoUri,
        lowerResolutionPhotoUrl = lowerResolutionPhotoUri
    )
}

fun Cluster.toEntity(): ClusterEntity {
    return ClusterEntity(
        localClusterId = localClusterId,
        globalClusterId = globalClusterId,
        location = location?.toEntity(),
        thumbnailId = thumbnail?.localPhotoId?.takeIf { it > 0 },
        photoCount = photoCount,
        visibility = visibility,
        publicPhotoCount = publicPhotoCount,
        privatePhotoCount = privatePhotoCount,
        friendPhotoCount = friendPhotoCount
    )
}

fun ClusterEntity.toDomain(photos: List<Photo>? = null, thumbnail: Photo? = null): Cluster {
    return Cluster(
        localClusterId = localClusterId,
        globalClusterId = globalClusterId,
        photos = photos,
        location = location?.toDomain(),
        thumbnail = thumbnail,
        photoCount = photoCount,
        visibility = visibility,
        publicPhotoCount = publicPhotoCount,
        privatePhotoCount = privatePhotoCount,
        friendPhotoCount = friendPhotoCount
    )
}
