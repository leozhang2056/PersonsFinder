# AI Log

> Records of 5 key AI-assisted development interactions for PersonsFinder.
> Each entry follows: **What I asked → What AI generated → What I changed**.

---

## 1. Haversine Distance & Nearby Search

**What I asked:** "Implement Haversine distance calculation to find people within a given radius."

**What AI generated:** A Kotlin Haversine formula using `kotlin.math` (sin/cos/atan2), earth radius 6371 km. Initial version loaded all locations into memory and filtered in code — O(n) on every query.

**What I changed:** Iterated through 4 versions to reach the final design:

| Version | Approach | Result |
|---------|----------|--------|
| V1 | In-memory Haversine | O(n) — timed out on 1M records |
| V2 | Native SQL with bounding box + `ORDER BY distance` + `LIMIT` | < 1s on 1M |
| V3 | Adaptive radius (5km → double → find N) | Handles sparse data |
| V4 | JDBC batch insert for seed data | ~23,800 inserts/s |

Added database index on `(latitude, longitude)`. Final `LocationsServiceImpl` supports two modes: fixed-radius and adaptive.

---

## 2. Prompt Injection Defense

**What I asked:** "Design prompt injection detection for the bio service. Users submit name, job title, hobbies — any could contain injection payloads."

**What AI generated:** A regex-based list of ~14 patterns. Initial approach was **strip matched text** and use remaining content.

**What I changed:** Replaced strip-and-continue with **intercept-and-reject**: if any pattern matches, the entire field is rejected (returns empty), and the caller uses a safe default. Strictly safer — no partial content survives. Decoupled `sanitize()` into its own method so regex can be swapped for LLM-based classification later.

---

## 3. Architecture Refactoring — SeedDataService

**What I asked:** "The controller is ~280 lines with seed logic mixed in. Extract it and use JDBC batch for performance."

**What AI generated:** `SeedDataService` with JDBC batch inserts, `RETURN_GENERATED_KEYS`, global city distribution (30 cities), progress logging.

**What I changed:** Also extracted `PersonAssembler` (entity→DTO conversion + validation), moved `PersonResponse` to its own file, switched controller dependency to `PersonsService` interface, and unified exception handler to return consistent `ApiResponse`. Final controller: ~190 lines of pure endpoint code.

---

## 4. Configurable Constants & Multi-Profile Setup

**What I asked:** "Extract magic numbers (80, 100, 5.0, 20000) and set up dev/prod/test profiles."

**What AI generated:** Constants to `companion object`, `@Value`-injected constructor params, `application-dev.properties` and `application-prod.properties`.

**What I changed:** Added `app.nearby.adaptive-max-radius` property (was hardcoded in controller). All tuning parameters externalized: `earth-radius-km`, `default-limit`, `adaptive-initial-radius`, `adaptive-padding`, `adaptive-max-radius`. Profiles now control database mode, H2 console, default radius, and JPA statistics independently.

---

## 5. Testing Non-Deterministic AI Services

**What I asked:** "How do you unit test an AI service that produces non-deterministic responses?" (Challenge bonus question)

**What AI suggested:** Two strategies: (1) deterministic mock — our mock uses fixed templates, same inputs → same outputs; (2) behavior contract — test what bio *should/not contain* rather than exact string.

**What I changed:** Wrote 32 AiBioService tests covering normal input (1-3 hobbies joined with commas + "and"), empty hobbies fallback, all 14 injection patterns individually, case-insensitive detection, all-hobbies-blocked fallback, job-title injection fallback, and async CompletableFuture verification. Total test suite: **85 tests** (32 + 9 + 20 + 23 + 1 context load).

---

## Summary

| Aspect | Approach |
|--------|----------|
| Distance calculation | Haversine via native SQL with bounding box pre-filter |
| Search strategy | Fixed radius or adaptive (5km → double → find N people) |
| Seed performance | JDBC batch insert, ~23,800 records/second |
| Injection defense | Intercept-and-reject with 14 regex patterns |
| PII protection | Name excluded from LLM call entirely |
| Architecture | Controller → Service → Repository, interface-based |
| Testing | 85 tests across 5 classes |
