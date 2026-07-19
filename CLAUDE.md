# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**PersonsFinder** is a Spring Boot + Kotlin backend challenge project — a REST API for a mobile app that finds people nearby. It uses an H2 in-memory database and is set up with a layered architecture (Controller → Service → Data). The project is currently **scaffolded with stubs** — most service methods and controller endpoints are `TODO("Not yet implemented")`.

## Build & Run Commands

```bash
# Build the project (compile + test)
./gradlew build

# Run tests only
./gradlew test

# Run a single test class
./gradlew test --tests "com.persons.finder.DemoApplicationTests"

# Start the application
./gradlew bootRun

# Clean build artifacts
./gradlew clean build
```

Tests use JUnit 5 via `useJUnitPlatform()`.

## Project Architecture

```
src/
├── main/
│   ├── kotlin/com/persons/finder/
│   │   ├── ApplicationStarter.kt       # @SpringBootApplication entry point
│   │   ├── data/
│   │   │   ├── Person.kt               # Person entity (id, name — incomplete vs reqs)
│   │   │   └── Location.kt             # Location entity (referenceId, lat, lon)
│   │   ├── domain/services/
│   │   │   ├── PersonsService.kt       # Interface: getById, save
│   │   │   ├── PersonsServiceImpl.kt   # Stub — all methods TODO
│   │   │   ├── LocationsService.kt     # Interface: addLocation, removeLocation, findAround
│   │   │   └── LocationsServiceImpl.kt # Stub — all methods TODO
│   │   └── presentation/
│   │       └── PersonController.kt     # REST controller, map stubs — all endpoints TODO
│   └── resources/
│       └── application.properties      # H2 in-memory DB config
└── test/
    └── kotlin/com/persons/finder/
        └── DemoApplicationTests.kt     # Single context-loads test
```

**Key characteristics:**
- Spring Boot 2.7.0, Kotlin 1.6.21, Java 11 (source compatibility)
- H2 in-memory database (`jdbc:h2:mem:testdb`) with JPA/Hibernate
- Gradle Kotlin DSL (`build.gradle.kts`)
- No JPA `@Repository` interfaces exist yet — persistence is entirely TODO
- No AI/bio generation service exists yet
- No prompt-injection or PII security measures exist yet
- The `Person` data class needs fields beyond `id`/`name` (job title, hobbies, bio, location)

## Required API Endpoints (from README challenge)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/persons` | Create person (name, job, hobbies, location) + generate AI bio |
| PUT | `/api/v1/persons/{id}/location` | Update person's location (lat, lon) |
| GET | `/api/v1/persons/nearby` | Find people near a location (lat, lon, radius) sorted by distance |

## What Needs to Be Built

1. **JPA Repositories** — `PersonRepository`, `LocationRepository` (or equivalent) extending `JpaRepository`
2. **Person domain model** — expand `Person` with job title, hobbies, bio, and location fields
3. **Service implementations** — fill in `PersonsServiceImpl` and `LocationsServiceImpl` (in-memory or JPA-backed)
4. **Haversine distance calculation** — for `findAround` spatial queries
5. **AI bio service** — interface + implementation (LLM call or mock) with prompt-injection safeguards
6. **Controller endpoints** — implement the POST/PUT/GET endpoints in `PersonController`
7. **Tests** — unit tests for services (especially the AI service), integration tests for endpoints
8. **Deliverables** — `AI_LOG.md`, `SECURITY.md` (prompt injection, PII privacy)

## Tech Stack

- Kotlin 1.6.21, Java 11
- Spring Boot 2.7.0 (Web, JPA starters)
- H2 Database (embedded)
- Gradle 7.x (wrapper included)
- JUnit 5 + Spring Boot Test
