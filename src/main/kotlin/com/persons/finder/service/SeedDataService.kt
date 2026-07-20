package com.persons.finder.service

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Statement
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

@Service
class SeedDataService(
    private val jdbcTemplate: JdbcTemplate,
    private val personsService: PersonsService
) {

    companion object {
        const val MAX_COUNT = 1_000_000
        const val BATCH_SIZE = 500
        const val LOG_INTERVAL_SECONDS = 3L
    }

    data class SeedResult(
        val inserted: Int,
        val elapsedSeconds: Long,
        val ratePerSecond: Long
    )

    // ==================== Data pools ====================

    private val jobTitles = listOf(
        "Software Engineer", "Chef", "UI Designer", "Data Analyst", "Product Manager",
        "DevOps Engineer", "Architect", "Marketing Manager", "Teacher", "Doctor", "Lawyer", "Designer"
    )

    private val namePool = listOf(
        "Alice", "Bob", "Carol", "David", "Eva", "Frank", "Grace", "Henry", "Ivy", "Jack",
        "Leo", "Mia", "Nick", "Olivia", "Paul", "Quinn", "Rose", "Sam", "Tina", "Victor"
    )

    private val hobbyPool = listOf(
        "hiking", "photography", "chess", "cooking", "traveling", "wine",
        "drawing", "yoga", "reading", "running", "music", "movies", "coffee", "swimming",
        "gaming", "cycling", "dancing"
    )

    private val cities = listOf(
        // China
        31.23 to 121.47, 39.90 to 116.40, 22.54 to 114.06, 23.13 to 113.26,
        30.57 to 104.07, 30.27 to 120.15, 32.06 to 118.80, 34.34 to 108.94,
        29.56 to 106.55, 43.83 to 125.32,
        // Global
        40.71 to -74.01, 34.05 to -118.24, 51.51 to -0.13, 48.86 to 2.35,
        35.68 to 139.69, 37.57 to 126.98, -33.87 to 151.21, 55.76 to 37.62,
        19.43 to -99.13, -23.55 to -46.63, 28.61 to 77.23, 1.35 to 103.82,
        25.20 to 55.27, -26.20 to 28.04, 41.90 to 12.50, -1.29 to 36.82,
        30.04 to 31.24, 19.08 to 72.88, 13.76 to 100.50, 52.52 to 13.40
    )

    private val rng = Random

    // ==================== Seed entry point ====================

    @Transactional
    fun seed(count: Int): SeedResult {
        require(count in 1..MAX_COUNT) { "count must be between 1 and $MAX_COUNT" }

        val start = Instant.now()
        var totalInserted = 0
        var lastLogTime = start

        for (i in 1..count step BATCH_SIZE) {
            val currentBatch = minOf(BATCH_SIZE, count - totalInserted)
            val (names, jobs, hobbyList, bios, lats, lngs) = generateBatch(currentBatch, totalInserted)

            val personIds = insertPersons(names, jobs, hobbyList, bios)
            insertLocations(personIds, lats, lngs)

            totalInserted += currentBatch
            totalInserted = logProgressIfDue(start, totalInserted, count, lastLogTime, now = Instant.now())
        }

        val elapsed = Duration.between(start, Instant.now()).seconds
        return SeedResult(
            inserted = totalInserted,
            elapsedSeconds = elapsed,
            ratePerSecond = if (elapsed > 0) totalInserted / elapsed else totalInserted.toLong()
        )
    }

    // ==================== Batch generation ====================

    private fun generateBatch(size: Int, offset: Int): BatchData {
        val names = mutableListOf<String>()
        val jobs = mutableListOf<String>()
        val hobbyList = mutableListOf<String>()
        val bios = mutableListOf<String>()
        val lats = mutableListOf<Double>()
        val lngs = mutableListOf<Double>()

        repeat(size) { idx ->
            val seq = offset + idx + 1
            names.add("${namePool[rng.nextInt(namePool.size)]}_$seq")

            val job = jobTitles[rng.nextInt(jobTitles.size)]
            jobs.add(job)

            val h = (1..rng.nextInt(1, 4)).map { hobbyPool[rng.nextInt(hobbyPool.size)] }.distinct()
            hobbyList.add(h.joinToString(", "))
            bios.add(personsService.generateSeedBio(job, h))

            val (lat, lon) = cities[rng.nextInt(cities.size)]
            lats.add(lat + (rng.nextDouble() - 0.5) * 0.1)
            lngs.add(lon + (rng.nextDouble() - 0.5) * 0.1)
        }

        return BatchData(names, jobs, hobbyList, bios, lats, lngs)
    }

    // ==================== JDBC inserts ====================

    private fun insertPersons(
        names: List<String>,
        jobs: List<String>,
        hobbyList: List<String>,
        bios: List<String>
    ): List<Long> {
        return jdbcTemplate.execute(org.springframework.jdbc.core.ConnectionCallback { conn ->
            conn.prepareStatement(
                "INSERT INTO persons (name, job_title, hobbies, bio) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                names.indices.forEach { idx ->
                    ps.setString(1, names[idx])
                    ps.setString(2, jobs[idx])
                    ps.setString(3, hobbyList[idx])
                    ps.setString(4, bios[idx])
                    ps.addBatch()
                }
                ps.executeBatch()

                val ids = mutableListOf<Long>()
                ps.generatedKeys.use { rs ->
                    while (rs.next()) ids.add(rs.getLong(1))
                }
                ids
            }
        })!!
    }

    private fun insertLocations(personIds: List<Long>, lats: List<Double>, lngs: List<Double>) {
        val locationArgs = personIds.mapIndexed { idx, pid ->
            arrayOf<Any>(pid, lats[idx], lngs[idx])
        }
        jdbcTemplate.batchUpdate(
            "INSERT INTO locations (person_id, latitude, longitude) VALUES (?, ?, ?)",
            locationArgs
        )
    }

    // ==================== Progress logging ====================

    private fun logProgressIfDue(start: Instant, totalInserted: Int, count: Int, lastLogTime: Instant, now: Instant): Int {
        if (Duration.between(lastLogTime, now).seconds >= LOG_INTERVAL_SECONDS) {
            val elapsed = Duration.between(start, now).seconds
            val rate = if (elapsed > 0) totalInserted / elapsed else totalInserted
            println("[seed] $totalInserted / $count inserted (${rate}/s, ${elapsed}s elapsed)")
        }
        return totalInserted
    }

    data class BatchData(
        val names: List<String>,
        val jobs: List<String>,
        val hobbyList: List<String>,
        val bios: List<String>,
        val lats: List<Double>,
        val lngs: List<Double>
    )
}
