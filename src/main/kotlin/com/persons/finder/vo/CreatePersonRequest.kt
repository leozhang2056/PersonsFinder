package com.persons.finder.vo

import io.swagger.v3.oas.annotations.media.Schema

data class LocationRequest(
    @Schema(description = "Latitude", example = "31.23")
    val latitude: Double,

    @Schema(description = "Longitude", example = "121.47")
    val longitude: Double
)

data class CreatePersonRequest(
    @Schema(description = "Person name", example = "John")
    val name: String,

    @Schema(description = "Job title", example = "Software Engineer")
    val jobTitle: String,

    @Schema(description = "List of hobbies", example = "[\"hiking\", \"photography\", \"chess\"]")
    val hobbies: List<String> = emptyList(),

    @Schema(description = "Latitude (alternative to location object)", example = "31.23")
    val latitude: Double? = null,

    @Schema(description = "Longitude (alternative to location object)", example = "121.47")
    val longitude: Double? = null,

    @Schema(description = "Location object (alternative to flat lat/lon)")
    val location: LocationRequest? = null
) {
    fun resolvedLatitude(): Double = location?.latitude ?: latitude ?: throw IllegalArgumentException("latitude is required")
    fun resolvedLongitude(): Double = location?.longitude ?: longitude ?: throw IllegalArgumentException("longitude is required")
}
