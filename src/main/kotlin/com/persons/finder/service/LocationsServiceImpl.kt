package com.persons.finder.service

import com.persons.finder.domain.Location
import com.persons.finder.mapper.LocationRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Service
class LocationsServiceImpl(
    private val locationRepository: LocationRepository,
    @Value("\${app.nearby.earth-radius-km:6371.0088}") private val earthRadiusKm: Double,
    @Value("\${app.nearby.default-limit:100}") private val defaultLimit: Int,
    @Value("\${app.nearby.adaptive-initial-radius:5.0}") private val adaptiveInitialRadius: Double,
    @Value("\${app.nearby.adaptive-padding:50}") private val adaptivePadding: Int
) : LocationsService {

    @Transactional
    override fun addLocation(location: Location) {
        validateLatitude(location.latitude)
        validateLongitude(location.longitude)
        val existing = locationRepository.findByPersonId(location.personId)
        if (existing != null) {
            existing.latitude = location.latitude
            existing.longitude = location.longitude
            locationRepository.save(existing)
        } else {
            locationRepository.save(location)
        }
    }

    @Transactional
    override fun addAllLocations(locations: List<Location>) {
        locationRepository.saveAll(locations)
    }

    @Transactional
    override fun removeLocation(personId: Long) {
        locationRepository.deleteByPersonId(personId)
    }

    override fun findByPersonId(personId: Long): Location? =
        locationRepository.findByPersonId(personId)

    override fun findAround(latitude: Double, longitude: Double, radiusInKm: Double): List<Location> {
        validateLatitude(latitude)
        validateLongitude(longitude)
        require(radiusInKm >= 0) { "radius must be >= 0" }
        return findAround(latitude, longitude, radiusInKm, defaultLimit)
    }

    override fun findAround(latitude: Double, longitude: Double, radiusInKm: Double, limit: Int): List<Location> {
        validateLatitude(latitude)
        validateLongitude(longitude)
        require(radiusInKm >= 0) { "radius must be >= 0" }
        require(limit > 0) { "limit must be > 0" }

        val deltaLat = Math.toDegrees(radiusInKm / earthRadiusKm)
        val deltaLon = Math.toDegrees(radiusInKm / (earthRadiusKm * cos(Math.toRadians(latitude))))

        val minLat = max(-90.0, latitude - deltaLat)
        val maxLat = min(90.0, latitude + deltaLat)
        val minLon = max(-180.0, longitude - deltaLon)
        val maxLon = min(180.0, longitude + deltaLon)

        val candidates = locationRepository.findWithinBounds(latitude, longitude, minLat, maxLat, minLon, maxLon, limit)

        // SQL already sorts by Haversine distance; re-filter precisely for corner cases
        return candidates
            .map { it to distanceInKm(latitude, longitude, it.latitude, it.longitude) }
            .filter { it.second <= radiusInKm }
            .sortedBy { it.second }
            .map { it.first }
    }

    override fun findAroundAdaptive(
        latitude: Double,
        longitude: Double,
        targetCount: Int,
        maxRadiusKm: Double
    ): Pair<List<Location>, Double> {
        validateLatitude(latitude)
        validateLongitude(longitude)
        require(targetCount > 0) { "targetCount must be > 0" }
        require(maxRadiusKm > 0) { "maxRadiusKm must be > 0" }

        var radius = adaptiveInitialRadius
        while (radius <= maxRadiusKm) {
            val result = findAround(latitude, longitude, radius, targetCount + adaptivePadding)
            if (result.size >= targetCount) {
                return result.take(targetCount) to radius
            }
            radius *= 2.0
        }

        val result = findAround(latitude, longitude, maxRadiusKm, targetCount + adaptivePadding)
        return result.take(targetCount) to maxRadiusKm
    }

    override fun distanceInKm(fromLatitude: Double, fromLongitude: Double, toLatitude: Double, toLongitude: Double): Double {
        val latDelta = Math.toRadians(toLatitude - fromLatitude)
        val lonDelta = Math.toRadians(toLongitude - fromLongitude)
        val fromLat = Math.toRadians(fromLatitude)
        val toLat = Math.toRadians(toLatitude)

        val a = sin(latDelta / 2).pow(2.0) + cos(fromLat) * cos(toLat) * sin(lonDelta / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusKm * c
    }

    private fun validateLatitude(latitude: Double) {
        require(latitude in -90.0..90.0) { "latitude must be between -90 and 90" }
    }

    private fun validateLongitude(longitude: Double) {
        require(longitude in -180.0..180.0) { "longitude must be between -180 and 180" }
    }
}
