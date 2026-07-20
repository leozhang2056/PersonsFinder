package com.persons.finder.config

import com.persons.finder.service.SeedDataService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class DataInitializer(
    private val seedDataService: SeedDataService,
    @Value("\${app.seed.enabled:false}") private val seedEnabled: Boolean,
    @Value("\${app.seed.count:1000}") private val seedCount: Int
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String) {
        if (!seedEnabled) {
            log.info("Auto-seed is disabled. Set app.seed.enabled=true in application.properties to enable.")
            return
        }
        log.info("Auto-seed enabled: inserting {} records...", seedCount)
        val start = System.currentTimeMillis()
        val result = seedDataService.seed(seedCount)
        val elapsed = System.currentTimeMillis() - start
        log.info("Auto-seed complete: {} records in {}ms ({} / s)",
            result.inserted, elapsed, result.ratePerSecond)
    }
}
