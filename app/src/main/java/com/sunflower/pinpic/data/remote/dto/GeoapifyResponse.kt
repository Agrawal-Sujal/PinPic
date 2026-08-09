package com.sunflower.pinpic.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class GeoapifyResponse(
    val type: String? = null,
    val features: List<GeoapifyFeature> = emptyList()
)

@Serializable
data class GeoapifyFeature(
    val type: String? = null,
    val properties: GeoapifyProperties? = null
)

@Serializable
data class GeoapifyProperties(
    val name: String? = null,
    val country: String? = null,
    val country_code: String? = null,
    val state: String? = null,
    val state_code: String? = null,
    val county: String? = null,
    val county_code: String? = null,
    val postcode: String? = null,
    val city: String? = null,
    val street: String? = null,
    val housenumber: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val formatted: String? = null,
    val address_line1: String? = null,
    val address_line2: String? = null,
    val result_type: String? = null,
    val distance: Double? = null,
    val category: String? = null,
    val rank: JsonElement? = null,
    val timezone: JsonElement? = null,
    val datasource: JsonElement? = null
)
