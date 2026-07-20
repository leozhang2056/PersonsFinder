package com.persons.finder

import com.persons.finder.domain.Location
import com.persons.finder.mapper.LocationRepository
import com.persons.finder.service.LocationsServiceImpl
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class LocationsServiceTest {

    @MockK
    private lateinit var locationRepository: LocationRepository

    private lateinit var locationsService: LocationsServiceImpl

    @BeforeEach
    fun setUp() {
        locationsService = LocationsServiceImpl(
            locationRepository = locationRepository,
            earthRadiusKm = 6371.0088,
            defaultLimit = 100,
            adaptiveInitialRadius = 5.0,
            adaptivePadding = 50
        )
    }

    @Test
    fun `distance between same point should be zero`() {
        val distance = locationsService.distanceInKm(40.7128, -74.0060, 40.7128, -74.0060)
        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun `distance between NYC and LA should be approximately 3944 km`() {
        val distance = locationsService.distanceInKm(40.7128, -74.0060, 34.0522, -118.2437)
        assertEquals(3944.0, distance, 20.0)
    }

    @Test
    fun `distance between equator and north pole should be approximately 10000 km`() {
        val distance = locationsService.distanceInKm(0.0, 0.0, 90.0, 0.0)
        assertEquals(10007.0, distance, 10.0)
    }

    @Test
    fun `distance should be symmetric`() {
        val d1 = locationsService.distanceInKm(52.5200, 13.4050, 48.8566, 2.3522)
        val d2 = locationsService.distanceInKm(48.8566, 2.3522, 52.5200, 13.4050)
        assertEquals(d1, d2, 0.001)
    }

    @Test
    fun `distance between Shanghai and Beijing should be approximately 1068 km`() {
        val distance = locationsService.distanceInKm(31.23, 121.47, 39.90, 116.40)
        assertEquals(1068.0, distance, 15.0)
    }

    @Test
    fun `distance between antipodal points should be approximately half the circumference`() {
        val distance = locationsService.distanceInKm(0.0, 0.0, 0.0, 180.0)
        assertEquals(20015.0, distance, 30.0)
    }

    @Test
    fun `findAround should return locations within radius`() {
        val center = Location(personId = 100, latitude = 40.0, longitude = -74.0)
        val nearby = Location(personId = 101, latitude = 40.1, longitude = -74.0)

        every { locationRepository.findWithinBounds(any(), any(), any(), any(), any(), any(), any()) } returns listOf(center, nearby)

        val result = locationsService.findAround(40.0, -74.0, 20.0)

        assertEquals(2, result.size)
        assertTrue(result.contains(center))
        assertTrue(result.contains(nearby))
    }

    @Test
    fun `findAround with zero radius should only return exact matches`() {
        val center = Location(personId = 100, latitude = 40.0, longitude = -74.0)

        every { locationRepository.findWithinBounds(any(), any(), any(), any(), any(), any(), any()) } returns listOf(center)

        val result = locationsService.findAround(40.0, -74.0, 0.0)

        assertEquals(listOf(center), result)
    }

    @Test
    fun `findAround should sort by distance ascending`() {
        val far = Location(personId = 101, latitude = 41.0, longitude = -74.0)
        val near = Location(personId = 102, latitude = 40.1, longitude = -74.0)

        every { locationRepository.findWithinBounds(any(), any(), any(), any(), any(), any(), any()) } returns listOf(far, near)

        val result = locationsService.findAround(40.0, -74.0, 200.0)

        assertEquals(2, result.size)
        assertEquals(near, result[0])
        assertEquals(far, result[1])
    }

    @Test
    fun `findAround with empty repository should return empty list`() {
        every { locationRepository.findWithinBounds(any(), any(), any(), any(), any(), any(), any()) } returns emptyList()

        val result = locationsService.findAround(40.0, -74.0, 100.0)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `findAround should throw on invalid latitude`() {
        assertThrows<IllegalArgumentException> {
            locationsService.findAround(100.0, 0.0, 100.0)
        }
    }

    @Test
    fun `findAround should throw on invalid longitude`() {
        assertThrows<IllegalArgumentException> {
            locationsService.findAround(0.0, 200.0, 100.0)
        }
    }

    @Test
    fun `findAround should throw on negative radius`() {
        assertThrows<IllegalArgumentException> {
            locationsService.findAround(0.0, 0.0, -1.0)
        }
    }

    @Test
    fun `addLocation should save when no existing location`() {
        val location = Location(personId = 1, latitude = 40.0, longitude = -74.0)
        every { locationRepository.findByPersonId(1) } returns null
        every { locationRepository.save(any()) } returns location

        locationsService.addLocation(location)

        verify { locationRepository.save(location) }
    }

    @Test
    fun `addLocation should update existing location when person already has one`() {
        val existing = Location(personId = 1, latitude = 40.0, longitude = -74.0)
        existing.id = 1
        val newLocation = Location(personId = 1, latitude = 34.0, longitude = -118.0)

        every { locationRepository.findByPersonId(1) } returns existing
        every { locationRepository.save(existing) } returns existing

        locationsService.addLocation(newLocation)

        assertEquals(34.0, existing.latitude)
        assertEquals(-118.0, existing.longitude)
        verify { locationRepository.save(existing) }
    }

    @Test
    fun `addLocation should throw on invalid latitude`() {
        val location = Location(personId = 1, latitude = 100.0, longitude = 0.0)
        assertThrows<IllegalArgumentException> {
            locationsService.addLocation(location)
        }
    }

    @Test
    fun `addLocation should throw on invalid longitude`() {
        val location = Location(personId = 1, latitude = 0.0, longitude = 200.0)
        assertThrows<IllegalArgumentException> {
            locationsService.addLocation(location)
        }
    }

    @Test
    fun `removeLocation should delegate to repository`() {
        every { locationRepository.deleteByPersonId(1) } just runs
        locationsService.removeLocation(1)
        verify { locationRepository.deleteByPersonId(1) }
    }

    @Test
    fun `findByPersonId should return location when exists`() {
        val location = Location(personId = 1, latitude = 40.0, longitude = -74.0)
        every { locationRepository.findByPersonId(1) } returns location

        val result = locationsService.findByPersonId(1)

        assertAll(
            { assertNotNull(result) },
            { assertEquals(40.0, result!!.latitude) },
            { assertEquals(-74.0, result!!.longitude) }
        )
    }

    @Test
    fun `findByPersonId should return null when not found`() {
        every { locationRepository.findByPersonId(999) } returns null
        val result = locationsService.findByPersonId(999)
        assertEquals(null, result)
    }

    @Test
    fun `addAllLocations should save all`() {
        val locations = listOf(
            Location(personId = 1, latitude = 40.0, longitude = -74.0),
            Location(personId = 2, latitude = 34.0, longitude = -118.0)
        )
        every { locationRepository.saveAll(locations) } returns locations
        locationsService.addAllLocations(locations)
        verify { locationRepository.saveAll(locations) }
    }
}
