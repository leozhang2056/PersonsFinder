package com.persons.finder.controller

import com.persons.finder.domain.Location
import com.persons.finder.domain.Person
import com.persons.finder.service.AiBioService
import com.persons.finder.service.LocationsService
import com.persons.finder.service.PersonsService
import com.persons.finder.service.SeedDataService
import com.persons.finder.vo.ApiResponse
import com.persons.finder.vo.CreatePersonRequest
import com.persons.finder.vo.LocationResponse
import com.persons.finder.vo.LocationUpdateRequest
import com.persons.finder.vo.NearbyPersonResponse
import com.persons.finder.vo.PersonAssembler.hobbiesList
import com.persons.finder.vo.PersonAssembler.toResponse
import com.persons.finder.vo.PersonAssembler.validateLatitude
import com.persons.finder.vo.PersonAssembler.validateLongitude
import com.persons.finder.vo.PersonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/persons")
class PersonController(
    private val personsService: PersonsService,
    private val locationsService: LocationsService,
    private val aiBioService: AiBioService,
    private val seedDataService: SeedDataService,
    @Value("\${app.nearby.default-radius:10}") private val defaultRadius: Double,
    @Value("\${app.nearby.adaptive-max-radius:20000.0}") private val maxAdaptiveRadius: Double
) {
    companion object {
        const val MAX_NAME_LENGTH = 100
        const val MAX_JOB_LENGTH = 100
        const val MAX_HOBBY_LENGTH = 80
        const val DEFAULT_TARGET_COUNT = 30
    }

    @PostMapping
    @Operation(summary = "Create a person", description = "Creates a new person with AI-generated bio based on job title and hobbies. Includes prompt injection protection.")
    fun createPerson(@RequestBody request: CreatePersonRequest): CompletableFuture<ResponseEntity<ApiResponse<PersonResponse>>> {
        require(request.name.isNotBlank()) { "name is required" }
        require(request.jobTitle.isNotBlank()) { "jobTitle is required" }

        val latitude = request.resolvedLatitude()
        val longitude = request.resolvedLongitude()
        validateLatitude(latitude)
        validateLongitude(longitude)

        val hobbies = request.hobbies.map { it.trim().take(MAX_HOBBY_LENGTH) }.filter { it.isNotBlank() }

        return aiBioService.generateBio(request.jobTitle, hobbies).thenApply { bio ->
            val saved = personsService.save(
                Person(
                    name = request.name.trim().take(MAX_NAME_LENGTH),
                    jobTitle = request.jobTitle.trim().take(MAX_JOB_LENGTH),
                    hobbies = hobbies.joinToString(", "),
                    bio = bio
                )
            )

            locationsService.addLocation(Location(personId = saved.id, latitude = latitude, longitude = longitude))

            ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(saved.toResponse(latitude, longitude)))
        }
    }

    @PutMapping("/{id}/location")
    @Operation(summary = "Update location", description = "Updates the current location of a person. Subsequent updates overwrite the previous location.")
    fun updateLocation(
        @Parameter(description = "Person ID", example = "1")
        @PathVariable id: Long,
        @RequestBody request: LocationUpdateRequest
    ): ApiResponse<PersonResponse> {
        validateLatitude(request.latitude)
        validateLongitude(request.longitude)
        val person = personsService.getById(id)
        locationsService.addLocation(Location(personId = person.id, latitude = request.latitude, longitude = request.longitude))
        return ApiResponse.success(person.toResponse(request.latitude, request.longitude))
    }

    @GetMapping("/nearby")
    @Operation(summary = "Nearby search", description = "Finds people near a given latitude/longitude within a radius. When radius is omitted, adaptive search expands from 5km until enough people are found.")
    fun findNearby(
        @Parameter(description = "Query latitude", example = "31.23")
        @RequestParam(required = false) latitude: Double?,
        @Parameter(description = "Query longitude", example = "121.47")
        @RequestParam(required = false) longitude: Double?,
        @Parameter(description = "Latitude alias (shorthand)", example = "31.23")
        @RequestParam(required = false) lat: Double?,
        @Parameter(description = "Longitude alias (shorthand)", example = "121.47")
        @RequestParam(required = false) lon: Double?,
        @Parameter(description = "Search radius in km. Omit for adaptive search", example = "10")
        @RequestParam(required = false) radius: Double?,
        @Parameter(description = "Target count (adaptive mode returns ~30 closest)", example = "30")
        @RequestParam(defaultValue = "30") count: Int,
        @Parameter(description = "Maximum results to return", example = "100")
        @RequestParam(defaultValue = "100") max: Int,
    ): ApiResponse<List<NearbyPersonResponse>> {
        val start = System.currentTimeMillis()
        val queryLatitude = latitude ?: lat ?: throw IllegalArgumentException("latitude is required")
        val queryLongitude = longitude ?: lon ?: throw IllegalArgumentException("longitude is required")
        validateLatitude(queryLatitude)
        validateLongitude(queryLongitude)
        require(count in 1..max) { "count must be between 1 and max" }
        require(max > 0) { "max must be > 0" }

        val targetCount = if (radius != null) max else count.coerceAtMost(DEFAULT_TARGET_COUNT)
        val (locations, usedRadius) = if (radius != null) {
            locationsService.findAround(queryLatitude, queryLongitude, radius, max) to radius
        } else {
            locationsService.findAroundAdaptive(queryLatitude, queryLongitude, targetCount, maxAdaptiveRadius)
        }

        val result = locations
            .mapNotNull { location ->
                runCatching {
                    val person = personsService.getById(location.personId)
                    NearbyPersonResponse(
                        id = person.id,
                        name = person.name,
                        jobTitle = person.jobTitle,
                        hobbies = person.hobbiesList(),
                        bio = person.bio,
                        location = LocationResponse(location.latitude, location.longitude),
                        distanceInKm = locationsService.distanceInKm(queryLatitude, queryLongitude, location.latitude, location.longitude)
                    )
                }.getOrNull()
            }
            .sortedBy { it.distanceInKm }
            .take(max)

        val elapsed = (System.currentTimeMillis() - start) / 1000.0
        return ApiResponse.success(result, elapsed)
    }

    @GetMapping
    @Operation(summary = "List all person IDs", description = "Returns a list of all person IDs in the database.")
    fun getAllPersonIds(): ApiResponse<List<Long>> = ApiResponse.success(personsService.findAll().map { it.id })

    @GetMapping("/{id}")
    @Operation(summary = "Get person details", description = "Returns full person details (including location and AI bio) by ID.")
    fun getPerson(
        @Parameter(description = "Person ID", example = "1")
        @PathVariable id: Long
    ): ApiResponse<PersonResponse> {
        val person = personsService.getById(id)
        val location = locationsService.findByPersonId(id)
        return ApiResponse.success(person.toResponse(location?.latitude, location?.longitude))
    }

    @RequestMapping("/seed", method = [RequestMethod.GET, RequestMethod.POST])
    @Operation(summary = "Seed test data", description = "Populates the database with globally distributed random data for performance testing. count=1000~1000000.")
    fun seedData(
        @Parameter(description = "Number of records to generate", example = "1000")
        @RequestParam(defaultValue = "1000") count: Int
    ): ApiResponse<Map<String, Any>> {
        val result = seedDataService.seed(count)
        return ApiResponse.success(mapOf(
            "inserted" to result.inserted,
            "elapsedSeconds" to result.elapsedSeconds,
            "ratePerSecond" to result.ratePerSecond
        ))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(exception: IllegalArgumentException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity.badRequest().body(ApiResponse.error(exception.message ?: "invalid request", 400))
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(exception: NoSuchElementException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(exception.message ?: "not found", 404))
    }
}
