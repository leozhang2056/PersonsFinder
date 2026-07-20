# AI Log

> This document records key interactions with AI (Claude Code) during the development of PersonsFinder.

---

## Interaction 1: Haversine Formula & Nearby Search Optimization

**Prompt:** "Implement Haversine distance calculation for the nearby search feature."

**AI Response:** Generated a Haversine formula implementation using `kotlin.math` functions with Earth's radius at 6371 km.

**Iterative optimization:**
- **V1** — Pure Kotlin Haversine in service layer, loaded all locations into memory and filtered/sorted in code. Worked for small datasets but O(n) scan on every query.
- **V2** — Moved Haversine into native SQL `@Query` with bounding box pre-filter: `WHERE lat BETWEEN min AND max AND lon BETWEEN min AND max ORDER BY distance`. Added `LIMIT` to cap results. Added `idx_locations_lat_lon` database index.
- **V3** — Added adaptive radius search: starts at 5km, doubles until enough results found. `count` parameter controls target result count.
- **V4** — Replaced JPA `saveAll()` with `JdbcTemplate.batchUpdate()` for seed data, achieving ~17,000 inserts/second.

**My Adjustment:** V1's pure in-memory approach didn't scale past 10k records. I refactored with AI to use SQL-level filtering and sorting, which brought 1M-record search time from timeout down to < 1s.

---

## Interaction 2: Prompt Injection Protection Design

**Prompt:** "Design a prompt injection detection system for the AI bio generation service."

**AI Response:** Provided a regex-based detection system that checks for common injection patterns like:
- `"ignore all instructions"`, `"ignore previous"`
- `"system prompt"`, `"developer message"`
- `"say '..."`, `"I'm hacked"`, `"jailbreak"`
- `"act as ..."`, `"forget your rules"`

When a pattern matches, the system strips the matched text and returns a sanitized version. If all content is stripped, a safe fallback template (`"collecting tiny moments"`) is used.

**My Adjustment:** I separated the sanitization logic into a dedicated `sanitize()` method, making it independent from the bio template generation. The architecture allows swapping regex-based detection for ML-based detection without changing the bio generation flow.

---

## Interaction 3: Architecture Extraction — SeedDataService

**Prompt:** "The controller is too large with seed logic mixed in. Extract it and use JdbcTemplate for batch performance."

**AI Response:** Extracted `SeedDataService` from `PersonController`, with:
- Pure JDBC batch inserts (bypasses JPA `IDENTITY` batch limitation)
- `RETURN_GENERATED_KEYS` to get person IDs for location inserts
- Global city distribution: 30 cities across 6 continents
- Progress logging every 3 seconds

**My Adjustment:** Also extracted `PersonAssembler` (entity→VO conversion + validation helpers). Code climate went from ~280 lines in the controller down to ~150, with clear responsibilities.

---

## Interaction 4: Adaptive Nearby Search

**Prompt:** "The nearby search times out on 1M records. Make it adaptive — start small, expand until enough results."

**AI Response:** Implemented `findAroundAdaptive()`:
- Starts at `adaptiveInitialRadius` (5km, configurable)
- Doubles radius each iteration
- Uses `targetCount + padding` in SQL LIMIT to avoid repeated queries
- Falls back to `adaptiveMaxRadius` (20000km)

**My Adjustment:** The initial version tried to query with `ORDER BY distance` on every iteration, which was slow. I configured `defaultLimit` and `adaptivePadding` as application properties so tuning doesn't require code changes.

---

## Interaction 5: Unit Testing Strategy for Non-Deterministic AI Service

**Prompt:** "Write unit tests for the AI bio service. How do you test a non-deterministic response?" (Bonus question)

**AI Response:** Two strategies:
1. **Deterministic mock** — Since `AiBioServiceImpl` uses fixed templates, same inputs produce same outputs.
2. **Behavior contract** — Test what the bio *should contain/not contain* rather than exact string matching.

**My Adjustment:** I wrote 7 unit tests plus additional ones for `PersonsService` (8 tests), `LocationsService` (13 tests), and `PersonControllerIntegrationTest` (18 tests), totaling 46 tests across 5 test classes.
