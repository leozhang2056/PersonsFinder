# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**PersonsFinder** is a Spring Boot + Kotlin backend challenge project — a REST API for a mobile app that finds people nearby. It uses an H2 file-based database and is set up with a layered architecture (Controller → Service → Repository → Entity). The project has been fully implemented with all REST endpoints, AI biography generation, Haversine distance calculation, and prompt-injection protection.

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
├── main/kotlin/com/persons/finder/
│   ├── ApplicationStarter.kt       # @SpringBootApplication entry point
│   ├── controller/
│   │   └── PersonController.kt     # REST endpoints (POST/PUT/GET)
│   ├── domain/
│   │   ├── Person.kt               # JPA Entity: id, name, jobTitle, hobbies, bio
│   │   └── Location.kt             # JPA Entity: id, personId, latitude, longitude
│   ├── mapper/
│   │   ├── PersonRepository.kt     # JpaRepository<Person, Long>
│   │   └── LocationRepository.kt   # JpaRepository<Location, Long>
│   ├── service/
│   │   ├── PersonsService.kt       # Interface + implementation (CRUD)
│   │   ├── PersonsServiceImpl.kt
│   │   ├── LocationsService.kt     # Interface + Haversine implementation
│   │   ├── LocationsServiceImpl.kt # findAround with distance calculation
│   │   ├── AiBioService.kt         # Interface for bio generation
│   │   └── AiBioServiceImpl.kt     # Mock implementation with prompt-injection sanitization
│   └── vo/                         # Value Objects / DTOs
│       ├── CreatePersonRequest.kt  # POST /persons request body
│       ├── LocationUpdateRequest.kt# PUT /persons/{id}/location request body
│       ├── LocationResponse.kt     # Location data in responses
│       ├── PersonResponse.kt       # Person data in responses
│       └── NearbyPersonResponse.kt # Nearby search result with distance
├── main/resources/
│   └── application.properties      # H2 file-based DB config
└── test/kotlin/com/persons/finder/
    ├── DemoApplicationTests.kt     # Context-loads test
    ├── AiBioServiceTest.kt         # Unit tests for bio generation (7 cases)
    └── PersonControllerIntegrationTest.kt  # Integration tests (7 ordered tests)
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/persons` | Create person with AI-generated bio (returns 201) |
| PUT | `/persons/{id}/location` | Update person's location |
| GET | `/persons` | List all person IDs |
| GET | `/persons/{id}` | Get person details with location |
| GET | `/persons/nearby` | Find people near lat/lon within radius (sorted by distance) |

## Code Formatting Standards

### Encoding
- **UTF-8 without BOM** for all source files — enforced via `.editorconfig`
- **CRLF line endings** (Windows) — enforced via `.gitattributes`
- All `.md` files containing emoji/non-ASCII text use UTF-8; never open/save them with ANSI encoding

### Enforced by `.editorconfig` (auto-detected by IntelliJ IDEA, VS Code, etc.)
- `charset = utf-8`
- `end_of_line = crlf`
- Kotlin: `indent_style = space`, `indent_size = 4`
- Gradle (`.kts`): `indent_style = tab`, `indent_size = 4`

### Enforced by `.gitattributes`
- `* text=auto` — Git auto-detects line endings
- `.kt`, `.kts`, `.md`, `.properties` — `eol=crlf`
- Binary files (`.png`, `.jar`) — `binary`

### Error Handling
- `IllegalArgumentException` → HTTP 400 (client error)
- `NoSuchElementException` → HTTP 404 (not found)
- All handled via `@ExceptionHandler` in `PersonController`

## Key Implementation Details

- **Haversine formula** in `LocationsServiceImpl` for great-circle distance (Earth radius ≈ 6371 km)
- **Prompt injection protection** in `AiBioServiceImpl` using regex pattern blocking
- **H2 file-based database** (`./data/persons_finder`) with auto DDL update
- **Manual input validation** via Kotlin `require()`
- **Two AI service implementations exist**: `AiBioServiceImpl` (active, in `service/`) and `MockBioService` (unused, in `ai/`) — clean up `domain/ai/` if no longer needed
