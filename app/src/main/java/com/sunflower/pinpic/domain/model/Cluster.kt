package com.sunflower.pinpic.domain.model

import com.sunflower.pinpic.data.local.entity.Visibility

data class Cluster(
    val localClusterId: Int = 0,
    val globalClusterId: String? = null,
    val photos: List<Photo>? = null,
    val location: Location? = null,
    val thumbnail: Photo? = null,
    val photoCount: Int = 0,
    val visibility: Visibility = Visibility.PRIVATE,
    val publicPhotoCount: Int = 0,
    val privatePhotoCount: Int = 0,
    val friendPhotoCount: Int = 0
) {
    fun calculateVisibility(): Visibility {
        return when {
            publicPhotoCount > 0 -> Visibility.PUBLIC
            friendPhotoCount > 0 -> Visibility.FRIENDS
            else -> Visibility.PRIVATE
        }
    }
}
