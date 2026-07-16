package com.sunflower.pinpic.data.mapper

import com.sunflower.pinpic.data.local.entity.ClusterPointEntity
import com.sunflower.pinpic.data.local.entity.PhotoEntity
import com.sunflower.pinpic.domain.model.ClusterPoint
import com.sunflower.pinpic.domain.model.Photo

fun Photo.toEntity(): PhotoEntity {
    return PhotoEntity(
        id = id,
        lat = lat,
        lng = lng,
        date = date,
        time = time,
        locationName = locationName,
        uri = uri,
        url = url,
        clusterId = clusterId
    )
}

fun PhotoEntity.toDomain(): Photo {
    return Photo(
        id = id,
        lat = lat,
        lng = lng,
        date = date,
        time = time,
        locationName = locationName,
        uri = uri,
        url = url,
        clusterId = clusterId
    )
}

fun ClusterPoint.toEntity(): ClusterPointEntity {
    return ClusterPointEntity(
        id = id,
        lat = lat,
        lng = lng,
        count = count,
        thumbnailUri = thumbnailUri
    )
}

fun ClusterPointEntity.toDomain(): ClusterPoint {
    return ClusterPoint(
        id = id,
        lat = lat,
        lng = lng,
        count = count,
        thumbnailUri = thumbnailUri
    )
}
