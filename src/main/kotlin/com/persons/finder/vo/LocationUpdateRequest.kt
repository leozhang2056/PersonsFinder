package com.persons.finder.vo

import io.swagger.v3.oas.annotations.media.Schema

data class LocationUpdateRequest(
    @Schema(description = "New latitude", example = "34.05")
    val latitude: Double,

    @Schema(description = "New longitude", example = "-118.24")
    val longitude: Double
)
