package com.persons.finder.service

import com.persons.finder.domain.Location

interface LocationsService {
    fun addLocation(location: Location)
    fun addAllLocations(locations: List<Location>)
    fun removeLocation(personId: Long)
    fun findByPersonId(personId: Long): Location?
    fun findAround(latitude: Double, longitude: Double, radiusInKm: Double): List<Location>
    fun findAround(latitude: Double, longitude: Double, radiusInKm: Double, limit: Int): List<Location>
    fun findAroundAdaptive(latitude: Double, longitude: Double, targetCount: Int, maxRadiusKm: Double): Pair<List<Location>, Double>
    fun distanceInKm(fromLatitude: Double, fromLongitude: Double, toLatitude: Double, toLongitude: Double): Double
}
