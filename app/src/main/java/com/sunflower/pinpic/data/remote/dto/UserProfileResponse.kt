package com.sunflower.pinpic.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileResponse(
    val id: String,
    val email: String? = null,
    val username: String,
    val name: String? = null,
    val mobileNumber: String? = null,
    val locale: String? = null,
    val pictureUrl: String? = null,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val isFollowing: Boolean = false
)
