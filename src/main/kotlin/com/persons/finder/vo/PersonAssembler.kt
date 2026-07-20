package com.persons.finder.vo

import com.persons.finder.domain.Person
object PersonAssembler {

    fun Person.toResponse(latitude: Double?, longitude: Double?): PersonResponse {
        return PersonResponse(
            id = id,
            name = name,
            jobTitle = jobTitle,
            hobbies = hobbiesList(),
            bio = bio,
            location = if (latitude != null && longitude != null) LocationResponse(latitude, longitude) else null
        )
    }

    fun Person.hobbiesList(): List<String> =
        hobbies.split(",").map { it.trim() }.filter { it.isNotBlank() }

    fun validateLatitude(latitude: Double) {
        require(latitude in -90.0..90.0) { "latitude must be between -90 and 90" }
    }

    fun validateLongitude(longitude: Double) {
        require(longitude in -180.0..180.0) { "longitude must be between -180 and 180" }
    }
}
