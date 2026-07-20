package com.persons.finder.mapper

import com.persons.finder.domain.Location
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface LocationRepository : JpaRepository<Location, Long> {
    fun findByPersonId(personId: Long): Location?
    fun deleteByPersonId(personId: Long)

    @Query(
        value = """
            SELECT l.*,
                   (6371.0088 * 2 * ASIN(SQRT(
                       POWER(SIN(RADIANS(l.latitude - :centerLat) / 2), 2) +
                       COS(RADIANS(:centerLat)) * COS(RADIANS(l.latitude)) *
                       POWER(SIN(RADIANS(l.longitude - :centerLon) / 2), 2)
                   ))) AS distance
            FROM locations l
            WHERE l.latitude BETWEEN :minLat AND :maxLat
              AND l.longitude BETWEEN :minLon AND :maxLon
            ORDER BY distance
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findWithinBounds(
        @Param("centerLat") centerLat: Double,
        @Param("centerLon") centerLon: Double,
        @Param("minLat") minLat: Double,
        @Param("maxLat") maxLat: Double,
        @Param("minLon") minLon: Double,
        @Param("maxLon") maxLon: Double,
        @Param("limit") limit: Int
    ): List<Location>
}
