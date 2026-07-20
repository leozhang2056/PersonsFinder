package com.persons.finder.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class DataInitializer(
    private val jdbcTemplate: JdbcTemplate,
    @Value("\${app.seed.enabled:false}") private val seedEnabled: Boolean,
    @Value("\${app.seed.count:1000000}") private val seedCount: Int
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    private val firstNames = listOf(
        "James","Mary","Robert","Patricia","John","Jennifer","Michael","Linda",
        "David","Elizabeth","William","Barbara","Richard","Susan","Joseph","Jessica",
        "Thomas","Sarah","Christopher","Karen","Charles","Lisa","Daniel","Nancy",
        "Matthew","Betty","Anthony","Margaret","Mark","Sandra","Donald","Ashley",
        "Steven","Kimberly","Paul","Emily","Andrew","Donna","Joshua","Michelle"
    )

    private val lastNames = listOf(
        "Smith","Johnson","Williams","Brown","Jones","Garcia","Miller","Davis",
        "Rodriguez","Martinez","Hernandez","Lopez","Gonzalez","Wilson","Anderson",
        "Thomas","Taylor","Moore","Jackson","Martin","Lee","Perez","Thompson",
        "White","Harris","Sanchez","Clark","Ramirez","Lewis","Robinson","Walker"
    )

    private val jobTitles = listOf(
        "Software Engineer","Data Scientist","Product Manager","UX Designer",
        "DevOps Engineer","Backend Developer","Frontend Developer","ML Engineer",
        "QA Engineer","Tech Lead","Solutions Architect","Cloud Engineer",
        "Mobile Developer","Security Analyst","Database Administrator",
        "Scrum Master","Business Analyst","Systems Engineer","Network Engineer"
    )

    private val hobbyPool = listOf(
        "hiking","reading","photography","cooking","gaming","painting","cycling",
        "swimming","running","chess","guitar","yoga","skiing","surfing","gardening",
        "bird watching","pottery","knitting","camping","fishing","dancing","writing",
        "meditation","trail running","rock climbing","diving","sailing","traveling",
        "blogging","filmmaking","woodworking","brewing","astronomy","origami",
        "bouldering","kayaking","archery","fencing","calligraphy","pottery"
    )

    private val bios = listOf(
        "Passionate about building things that matter.",
        "Turning coffee into code since day one.",
        "Loves solving hard problems with simple solutions.",
        "Lifelong learner, tech enthusiast, and cat person.",
        "Building the future one commit at a time.",
        "Curious mind with a love for data and design.",
        "Making the world a better place through software.",
        "Occasional hacker, full-time dreamer.",
        "Driven by curiosity, fueled by deadlines.",
        "Code by day, adventure by night."
    )

    override fun run(vararg args: String) {
        if (!seedEnabled) return

        val count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM persons", Long::class.java) ?: 0L
        if (count > 0) {
            log.info("Database already has {} records, skipping seed", count)
            return
        }

        log.info("Seeding {} persons + locations ...", seedCount)
        val start = System.currentTimeMillis()

        jdbcTemplate.execute("SET DB_CLOSE_DELAY -1")
        jdbcTemplate.execute("ALTER TABLE persons ALTER COLUMN ID RESTART WITH 1")
        jdbcTemplate.execute("ALTER TABLE locations ALTER COLUMN ID RESTART WITH 1")

        val batchPersons = """
            INSERT INTO persons (name, job_title, hobbies, bio) VALUES (?, ?, ?, ?)
        """.trimIndent()

        val batchLocations = """
            INSERT INTO locations (person_id, latitude, longitude) VALUES (?, ?, ?)
        """.trimIndent()

        val BATCH = 5000
        var personId = 0L

        for (offset in 0 until seedCount step BATCH) {
            val currentBatch = minOf(BATCH, seedCount - offset)
            val currentPersonIds = (1..currentBatch).map { ++personId }

            val persons = currentPersonIds.map {
                val firstName = firstNames[Random.nextInt(firstNames.size)]
                val lastName = lastNames[Random.nextInt(lastNames.size)]
                val job = jobTitles[Random.nextInt(jobTitles.size)]
                val hobbies = (1..Random.nextInt(1, 6))
                    .map { hobbyPool[Random.nextInt(hobbyPool.size)] }
                    .distinct()
                    .joinToString(", ")
                listOf<Any>("$firstName $lastName", job, hobbies, bios[Random.nextInt(bios.size)])
            }

            jdbcTemplate.batchUpdate(batchPersons, persons, currentBatch) { ps, row ->
                ps.setString(1, row[0] as String)
                ps.setString(2, row[1] as String)
                ps.setString(3, row[2] as String)
                ps.setString(4, row[3] as String)
            }

            val locations = currentPersonIds.map {
                val lat = Random.nextDouble(-90.0, 90.0)
                val lon = Random.nextDouble(-180.0, 180.0)
                listOf<Any>(it, lat, lon)
            }

            jdbcTemplate.batchUpdate(batchLocations, locations, currentBatch) { ps, row ->
                ps.setLong(1, row[0] as Long)
                ps.setDouble(2, row[1] as Double)
                ps.setDouble(3, row[2] as Double)
            }
        }

        val elapsed = System.currentTimeMillis() - start
        log.info("Seeded {} records in {}ms", seedCount, elapsed)
    }
}
