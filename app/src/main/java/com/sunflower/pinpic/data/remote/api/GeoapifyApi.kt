package com.sunflower.pinpic.data.remote.api

import com.sunflower.pinpic.data.remote.dto.GeoapifyResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GeoapifyApi {
    @GET("v1/geocode/reverse")
    suspend fun getReverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("apiKey") apiKey: String,
        @Query("format") format: String = "geojson"
    ): GeoapifyResponse
}
