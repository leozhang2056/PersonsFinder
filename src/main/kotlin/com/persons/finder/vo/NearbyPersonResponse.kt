package com.persons.finder.vo

data class NearbyPersonResponse(
    val id: Long,
    val name: String,
    val jobTitle: String,
    val hobbies: List<String>,
    val bio: String,
    val location: LocationResponse?,
    val distanceInKm: Double
)
