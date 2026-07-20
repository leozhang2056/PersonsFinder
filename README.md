# 👥 Persons Finder – Backend Challenge (AI-Augmented Edition)

Welcome to the **Persons Finder** backend challenge! This project simulates the backend for a mobile app that helps users find people around them, built with **Kotlin + Spring Boot 2.7**.

**Context:** At our company, we believe AI is a tool, not a replacement. We want to see how you leverage AI to code faster, think deeper, and build secure systems.

---

## 📌 Original Challenge Requirements

> Below is the original challenge prompt. ✅ marks show how each requirement was fulfilled in this implementation.

### ➕ `POST /persons` ✅
Create a new person.
*   **Input:** Name, Job Title, Hobbies, Location (lat/lon). ✅
*   **AI Integration:** Generates a **short, quirky bio** based on job + hobbies. ✅
    *   Mock implementation (`AiBioServiceImpl`) — architecture supports swapping to real LLM. ✅

### ✏️ `PUT /persons/{id}/location` ✅
Update a person's current location. ✅

### 🔍 `GET /persons/nearby` ✅
Find people around a query location (lat, lon, radius). ✅
*   **Output:** List of persons (including the AI bio), sorted by distance. ✅

### 🤖 AI Challenge
*   **AI Usage:** `AI_LOG.md` documents 5 key AI collaboration interactions. ✅
*   **Prompt Injection Protection:** `AiBioServiceImpl.sanitize()` blocks injection patterns. ✅
*   **SECURITY.md:** Discusses input sanitization and PII privacy risks. ✅

### 📦 Expected Output
*   **Code:** Clean Controller/Service/Repository layered architecture. ✅
*   **Storage:** H2 in-memory database (file mode available). ✅
*   **Docs:** `README.md`, `AI_LOG.md`, `SECURITY.md`. ✅

### 🧪 Bonus Points
*   **Scalability:** 1M records seeded, nearby search benchmarked (< 1s). ✅
*   **Clean Code:** DDD-inspired package structure with interfaces. ✅
*   **Testing:** 85 unit + integration tests (32 AI service tests covering non-deterministic behavior). ✅

### 📬 Submission
> Repository: https://github.com/leozhang2056/PersonsFinder.git

---

## 📦 Project Structure

```
src/
├── main/kotlin/com/persons/finder/
│   ├── ApplicationStarter.kt          # Entry point + root → Swagger redirect
│   ├── controller/
│   │   └── PersonController.kt        # REST API controller
│   ├── domain/
│   │   ├── Person.kt                  # Person entity
│   │   └── Location.kt                # Location entity (with lat/lon index)
│   ├── mapper/
│   │   ├── PersonRepository.kt        # Person JPA repository
│   │   └── LocationRepository.kt      # Location JPA repository (Haversine SQL)
│   ├── service/
│   │   ├── PersonsService.kt          # Person service interface
│   │   ├── PersonsServiceImpl.kt      # Person service implementation
│   │   ├── LocationsService.kt        # Location service interface
│   │   ├── LocationsServiceImpl.kt    # Location service (adaptive search)
│   │   ├── AiBioService.kt            # AI bio generation interface
│   │   ├── AiBioServiceImpl.kt        # AI bio generation (with injection defense)
│   │   └── SeedDataService.kt         # Bulk data seeding
│   └── vo/
│       ├── ApiResponse.kt             # Unified API response wrapper
│       ├── PersonAssembler.kt         # Entity→VO converter + validators
│       ├── CreatePersonRequest.kt     # Create person request DTO
│       ├── LocationUpdateRequest.kt   # Location update request DTO
│       ├── PersonResponse.kt          # Person response DTO
│       ├── LocationResponse.kt        # Location response DTO
│       └── NearbyPersonResponse.kt    # Nearby search response DTO
├── test/kotlin/com/persons/finder/
│   ├── AiBioServiceTest.kt            # AI service unit tests (32)
│   ├── PersonsServiceTest.kt          # Person service unit tests (9)
│   ├── LocationsServiceTest.kt        # Location service unit tests (20)
│   ├── PersonControllerIntegrationTest.kt  # Integration tests (23)
│   └── DemoApplicationTests.kt        # Context load test
└── resources/
    └── application.properties         # App configuration
```

### Architecture

```
Controller (REST endpoints)
    ↓
Service (business logic)
    ↓
Repository (JPA data access)
    ↓
H2 Database (in-memory / file)
```

---

## 🚀 Quick Start

### Prerequisites

- **JDK 11+** (JDK 17 recommended)
- Gradle (or use the built-in `gradlew` / `gradlew.bat`)

### Build

```bash
./gradlew build          # Linux / Mac / Git Bash
gradlew.bat build        # Windows CMD
```

### Run

```bash
./gradlew bootRun
```

Access the application at:
- 🌐 **API**: `http://localhost:5000` (redirects to Swagger)
- 📖 **Swagger UI**: `http://localhost:5000/swagger-ui/index.html`

---

## 🔌 API Endpoints

All responses use a unified format:

```json
{
  "success": true,
  "code": 200,
  "data": { ... },
  "runningTime": 0.123,
  "message": null
}
```

### 1️⃣ Create a person

```http
POST /persons
Content-Type: application/json

{
  "name": "John Doe",
  "jobTitle": "Software Engineer",
  "hobbies": ["hiking", "photography", "chess"],
  "latitude": 40.7128,
  "longitude": -74.006
}
```

**Response**: `201 Created` — includes an AI-generated quirky bio based on job + hobbies

> Location can be provided via top-level `latitude`/`longitude` or a `location` object.

### 2️⃣ Get all person IDs

```http
GET /persons
```

### 3️⃣ Get person by ID

```http
GET /persons/{id}
```

### 4️⃣ Update location

```http
PUT /persons/{id}/location
Content-Type: application/json

{
  "latitude": 34.0522,
  "longitude": -118.2437
}
```

Repeated updates overwrite the previous location.

### 5️⃣ Nearby search (core feature)

```http
# Adaptive radius (finds ~30 nearest people)
GET /persons/nearby?latitude=40.71&longitude=-74.01

# Fixed radius 10km
GET /persons/nearby?latitude=40.71&longitude=-74.01&radius=10

# Custom count
GET /persons/nearby?latitude=40.71&longitude=-74.01&count=50
```

**Strategy**:
- **Adaptive** (no radius): starts at 5km, doubles until enough results, max 20000km
- **Fixed** (with radius): searches within radius, `ORDER BY distance LIMIT max`

### 6️⃣ Seed test data

```http
POST /persons/seed?count=1000        # Small batch
POST /persons/seed?count=1000000     # 1M performance test
```
> Console outputs progress every 3 seconds:
> ```
> [seed] 49500 / 1000000 inserted (1178/s, 42s elapsed)
> ```

### 7️⃣ Multi-profile configuration

```properties
# Default (shared config)
app.nearby.default-radius=10

# Dev profile (application-dev.properties)
app.nearby.default-radius=5

# Prod profile (application-prod.properties)
app.nearby.default-radius=20
```

Run with profile:
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

> ⏳ **On first start** the app auto-inserts **1,000,000 records** (~60 seconds).
> Watch the console for progress output like:
> ```
> [seed] 49500 / 1000000 inserted (1178/s, 42s elapsed)
> ```
> Subsequent starts skip seeding because data already exists.

---

## ✅ Testing Methods

### 🎯 IDEA HTTP Client (dev)

Open **`test-api.http`** in IntelliJ IDEA — click the ▶ green arrow next to any request to send it immediately.

### 🌐 Swagger UI (testers)

Open `http://localhost:5000/swagger-ui/index.html`:
- Every endpoint has **descriptions** and **example values**
- Click **"Try it out"** → auto-fills example data → **"Execute"**

### 🖥️ Smoke test script (CI/acceptance)

```bash
# Linux / Git Bash
bash test-api.sh

# Windows — double-click
test-api.bat
```

Automatically tests 21 scenarios:

```
1️⃣ POST /persons — create person              ✅
2️⃣ GET /persons — get all IDs                  ✅
3️⃣ GET /persons/{id} — person detail           ✅
4️⃣ PUT /persons/{id}/location — update location ✅
5️⃣ GET /persons/nearby — nearby search         ✅
6️⃣ GET /persons/seed — seed data               ✅
7️⃣ 404 error handling                          ✅
8️⃣ 400 validation error                        ✅
9️⃣ Infrastructure (Swagger, H2 Console)        ✅
🔟 Prompt injection defense                    ✅
```

---

## ⚙️ Database

Uses **H2 in-memory** by default (fast, resets on restart). Switch to file mode in `application.properties`:

```properties
# In-memory (default)
spring.datasource.url=jdbc:h2:mem:persons_finder;DB_CLOSE_DELAY=-1

# File mode (persistent)
# spring.datasource.url=jdbc:h2:file:./data/persons_finder;AUTO_SERVER=TRUE
```

### H2 Console

- **URL**: `http://localhost:5000/h2-console`
- **JDBC URL**: `jdbc:h2:mem:persons_finder`
- **User**: `sa`
- **Password**: `password`

This completes the database section. For more details on connecting from IntelliJ IDEA, see the JDBC info above.

### Database Connection Details

| Field | Value |
|-------|-------|
| **Type** | H2 (in-memory / file) |
| **Driver** | `org.h2.Driver` |
| **JDBC URL (memory)** | `jdbc:h2:mem:persons_finder;DB_CLOSE_DELAY=-1` |
| **JDBC URL (file)** | `jdbc:h2:file:./data/persons_finder;AUTO_SERVER=TRUE` |
| **Username** | `sa` |
| **Password** | `password` |
| **Console** | `http://localhost:5000/h2-console` |

---

## 🧪 Running Tests

```bash
./gradlew test
```

| Test class | Count | Scope |
|-----------|-------|-------|
| `AiBioServiceTest` | 32 | Normal input, empty hobbies, injection defense, determinism |
| `PersonsServiceTest` | 9 | CRUD, existence check, batch save, seed bio |
| `LocationsServiceTest` | 20 | Distance calc (6), nearby search (7), CRUD (7) |
| `PersonControllerIntegrationTest` | 23 | End-to-end integration |

---

## 🛡️ Security

### Prompt Injection Defense

`AiBioServiceImpl.sanitize()` uses regex pattern matching to block common injection attempts (`"ignore all instructions"`, `"say 'I am hacked'"`, etc.). Injected text is stripped from the bio output. See `SECURITY.md`.

### PII Privacy

See `SECURITY.md` for discussion on:
- Input sanitization before sending to LLM
- Privacy risks of sending PII (name, location) to third-party models
- High-security banking app architecture

---

## 📊 Performance (1M records, H2 in-memory)

### Data Insertion

| Metric | Value |
|--------|-------|
| Total inserted | 1,000,000 persons + 1,000,000 locations |
| Total time | **42 seconds** |
| Insert rate | **23,809 records/second** |
| Method | JDBC batch insert (bypasses JPA, 500 per batch) |

### Nearby Search Benchmarks

| Scenario | Radius | Results | Latency |
|----------|--------|---------|---------|
| Adaptive (default) | Auto-expand | 30 | **0.35s** |
| Adaptive (10) | Auto-expand | 10 | **0.34s** |
| Fixed 5km | 5km | 100 | **0.47s** |
| Fixed 10km | 10km | 100 | **0.48s** |
| Fixed 50km | 50km | 100 | **0.46s** |
| Fixed 100km | 100km | 100 | **0.50s** |
| Fixed 1000km | 1000km | 100 | **2.25s** |
| Fixed 2000km | 2000km | 100 | **4.15s** |
| Fixed 5000km | 5000km | 100 | **4.93s** |

### Current Bottlenecks

1. **N+1 query problem** — nearby endpoint queries locations, then fetches each person individually. 100 results = 1 location query + 100 person queries.
2. **Large bounding box** — 1000km+ radius means H2 scans many rows for distance calculation.
3. **Dual Haversine** — SQL computes distance in native query, then Kotlin re-computes for re-filtering.

### Optimization Roadmap

| Optimization | Expected Impact | Difficulty |
|-------------|----------------|------------|
| JOIN query (eliminate N+1) | 50%+ latency reduction | Medium |
| Redis GeoHash index | Sub-ms for large radius | High |
| Spatial index (PostGIS / R-Tree) | <100ms for 1000km | High |
| Elasticsearch | Geofencing + full-text search | High |
| Field pruning (drop bio) | Lower serialization cost | Low |

---

## 🤖 AI Collaboration Log

See `AI_LOG.md` for 5 key AI interactions:
1. Haversine formula implementation & nearby search optimization
2. Prompt injection defense design
3. Architecture extraction (SeedDataService)
4. Adaptive nearby search
5. Unit testing strategy for non-deterministic AI services

---

## 📬 Submission

Submit your repository link. We will review your code, `AI_LOG.md`, and `SECURITY.md`.
