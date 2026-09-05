# Employee Service

## Purpose

Employee records, search, employee details, the six attrition-analysis dimensions (US-11 → US-16), and flagging an employee for HR follow-up (which kicks off the [Kafka flow](kafka.md)). This service gets particular attention because it's the one with the least conventional architecture in the project: **it has no database of its own.**

**Why this is a separate service rather than, say, folding "employee data" into User Profile Service:** it has an entirely different data source and access pattern — it proxies a third-party system live, on every request, with no persistence, while User Profile owns a real local table it fully controls. Combining the two would mean one service straddling two very different responsibilities (owning data vs. proxying someone else's), and would tie this project's uptime to the Survey API's uptime for *every* feature, not just employee browsing. Keeping it separate means a Survey API outage only breaks employee/attrition features — login, profile, and notifications keep working.

**What this service owns:** the mapping/aggregation logic that turns raw Survey API data into this project's own `EmployeeDto` shape, and the six attrition calculations. **What it explicitly does not own:** any employee data at rest (no database, no cache), and no security/authorization logic of its own (see below) — both are handled elsewhere on purpose.

## Why Employee Service has no database

```
External Survey API
        │  REST (Feign)
        ▼
  Employee Service   ← no DB
        │  REST
        ▼
    API Gateway
        │  REST
        ▼
     Frontend
```

Employee data isn't owned by this application at all — it's owned by an external **Survey API** (part of the case study this project is based on), which acts as the system of record. Employee Service is a **stateless proxy/aggregator** in front of it: every request (`getAllEmployees`, `findByProperty`, `getEmployeeById`, every attrition-analysis endpoint) calls the Survey API live and maps/aggregates the response — nothing is cached or persisted locally. This is a deliberate architectural choice, not a missing feature: storing a local copy would mean keeping it in sync with the Survey API, which the project doesn't need to solve.

One consequence: attrition-analysis endpoints (`/employees/analysis/**`) recompute their aggregation **on every call** by fetching the full employee list and grouping in memory (`aggregateAttritionBy` in `EmployeeService`) — there's no persisted rollup table.

**Trade-off this creates, worth being able to name in a Q&A:** correctness (results always reflect the Survey API's current data, with no staleness or cache-invalidation problem to get wrong) is bought at the cost of doing real work — a full employee fetch plus an in-memory grouping — on every single analysis request, even if called twice in a row with no data change in between. For this project's scale (a case-study dataset, not millions of live employee records) that trade is clearly the right one; it would need revisiting (e.g. a short-lived cache) only if the Survey API dataset or request volume grew enough for repeated full-fetches to become a real cost.

## Architecture / packages

| Package | Contents |
|---|---|
| `controller` | `EmployeeController` — every HTTP endpoint this service exposes |
| `service` | `EmployeeService` — all business logic, including the six attrition dimensions |
| `client` | `SurveyApiClient` (Feign, to the external Survey API), `SurveyEmployeeResponse` (the Survey API's own response shape) |
| `mapper` | `EmployeeMapper` — `SurveyEmployeeResponse` → `EmployeeDto` |
| `dto` | `EmployeeDto`, `AttritionAnalysisDto`, `FlagEmployeeRequest`, `ErrorResponse` |
| `event` | `EmployeeFlaggedEvent`, `EmployeeFlaggedEventProducer` (Kafka) |
| `security` | `JwtService` — verify-only, used only to read the caller's email off the token on the flag endpoint |
| `exception` | `SurveyApiException`, `UnauthenticatedException`, `EventPublicationException`, `GlobalExceptionHandler` |

Notably **no `SecurityConfig`, no Spring Security dependency at all** — access control for this service's routes is enforced entirely at the API Gateway (see [api-gateway.md](api-gateway.md)), not here. The one place this service reads a JWT itself is the flag endpoint, and only to extract the caller's identity for the outgoing event, not to authorize the request.

**Why this service breaks the "every service double-checks auth itself" pattern used by authentication-service and user-profile-service:** those two services guard sensitive, service-owned data (credentials, profile records) where defense-in-depth is worth the extra dependency and code. This service has no data of its own to protect at the persistence layer — it's a pass-through to a public-shaped dataset — so adding a full Spring Security filter chain here would be extra complexity without a matching security benefit; the Gateway's enforcement is the single source of truth for whether a caller is allowed to reach `/employees/**` at all. The one JWT read that *does* happen here (on the flag endpoint) isn't an authorization check — it's just extracting "who is this" to attribute the flag, which is a business-logic need, not a gate.

## Feign communication with the Survey API

```java
@FeignClient(name = "survey-api", url = "${survey-api.base-url}")
public interface SurveyApiClient {
    @GetMapping("/survey") List<SurveyEmployeeResponse> getAllEmployees();
    @GetMapping("/survey") List<SurveyEmployeeResponse> findByProperty(@SpringQueryMap Map<String, Object> query);
    @GetMapping("/survey/{id}") SurveyEmployeeResponse getEmployeeById(@PathVariable("id") String id);
}
```

Unlike the internal Feign clients elsewhere in this project (which resolve a target via Eureka's `name`), this one uses an explicit `url` (`survey-api.base-url`, default `http://localhost:3232`, overridden in Docker to `http://host.docker.internal:3232`) — the Survey API isn't a Eureka-registered service in this project, it's an external system reached by a configured address. Every `EmployeeService` method wraps its Feign call in a `try/catch FeignException`, translating a `404` into an empty `Optional` (for `getEmployeeById`) or a generic `SurveyApiException` (→ an HTTP error) for anything else.

External Survey API models (`SurveyEmployeeResponse`, with the Survey API's own field naming, e.g. `EmployeeID`, `DistanceFromHome_km`) are **never exposed directly to the frontend** — `EmployeeMapper.toEmployeeDto` translates every response into this project's own `EmployeeDto` shape first.

**Why the mapping layer exists at all, rather than just forwarding the Survey API's JSON:** two reasons. First, this project's own naming/casing conventions (`employeeId`, `distanceFromHomeKm`) shouldn't be dictated by an external system's field names — if the Survey API ever changed its response shape, only `EmployeeMapper` would need updating, not the frontend or every controller. Second, this is what "don't expose external API models directly to the frontend" (a project-wide rule) actually means in code: a translation boundary at the one place data crosses from an external system into this application.

**What happens on a Survey API failure, concretely:** every `EmployeeService` method that calls `SurveyApiClient` wraps it in `try { ... } catch (FeignException ex)`. For `getEmployeeById`, a `FeignException.NotFound` (the Survey API's own 404) becomes an empty `Optional` — indistinguishable, from the controller's point of view, from "this ID never existed." Any *other* Feign failure (connection refused, timeout, 500 from the Survey API) becomes a `SurveyApiException`, which `GlobalExceptionHandler` maps to `503 Service Unavailable` — correctly signaling "the thing we depend on is down," not "you sent a bad request" — so a Survey API outage surfaces to the frontend as a clear, distinguishable error rather than a hang or a generic 500 with no context. `EventPublicationException` (a Kafka publish failure on the flag endpoint) is mapped the same way, for the same reason: it's this service's own downstream dependency failing, not the caller's fault.

## Endpoints

| Method | Path | Auth (Gateway) | Purpose |
|---|---|---|---|
| GET | `/employees` | Yes | All employees |
| GET | `/employees?property=X&value=Y` | Yes | Exact-match single-field search (both params required together) |
| GET | `/employees/{id}` | Yes | One employee; `404` if not found |
| POST | `/employees/{id}/flag` | Yes | `{comment}` → publishes a Kafka event, `202 Accepted` |
| GET | `/employees/analysis/department` | **No** (Guest-visible, US-21/US-11) | Attrition by department |
| GET | `/employees/analysis/job-role` | **No** (US-12) | Attrition by job role |
| GET | `/employees/analysis/compensation` | **No** (US-13) | Attrition by salary band |
| GET | `/employees/analysis/demographics` | **No** (US-14) | Attrition by gender |
| GET | `/employees/analysis/work-life-balance` | **No** (US-15) | Attrition by overtime |
| GET | `/employees/analysis/career-progression` | **No** (US-16) | Attrition by years-since-promotion band |

"Auth" here reflects the **Gateway's** `permitAll`/`authenticated` rules, not anything enforced in this service itself.

**`GET /employees/{id}`, end to end (a representative authenticated read):**

```
Frontend → GET /employees/{id}  (Authorization: Bearer <token>)
  → Gateway: verifies JWT (protected route) → routes to employee-service
    → EmployeeController.getEmployee(id)
      → EmployeeService.getEmployeeById(id)
        → SurveyApiClient.getEmployeeById(id)  — Feign call to the external Survey API
          — 404 from Survey API → caught, returns Optional.empty()
          — other failure → SurveyApiException thrown
        → EmployeeMapper.toEmployeeDto(surveyResponse)  — on success
  ← 200 EmployeeDto           (found)
  ← 404 {"message": "Employee not found"}   (not found)
  ← 503 (via GlobalExceptionHandler)         (Survey API failure)
```

Note this service never re-validates the JWT itself here — by the time this controller method runs, the Gateway has already decided the request is allowed through; this service just does the work.

## The six attrition dimensions (US-11 → US-16)

All six share one private helper, `aggregateAttritionBy(groupKeyExtractor)`, which fetches the full employee list, groups it by the given key into a `TreeMap` (alphabetical/sorted group labels), and for each group computes `{groupLabel, totalCount, attritionCount, attritionRate}` (`attritionRate` = attrition count ÷ group size × 100).

| Dimension | Backlog term | Field actually used | Why (per the code's own comments) |
|---|---|---|---|
| US-11 Department | Department | `department`, or `"Unknown"` if blank | Direct field match — no judgment call |
| US-12 Job Role | Job Role | `jobRole`, or `"Unknown"` | Direct field match |
| US-13 Compensation | Compensation | `salary`, bucketed into $50,000 bands (e.g. `$50000-$99999`) | "Compensation" isn't a specific field in the backlog; salary is the obvious candidate, bucketed since it's continuous — the band width is an engineering decision, not a documented requirement |
| US-14 Demographics | Demographics | `gender`, or `"Unknown"` | "Demographics" isn't tied to a specific field; gender is the simplest, lowest-cardinality choice — a judgment call. Age, ethnicity, marital status, and state were equally plausible and weren't chosen |
| US-15 Work-Life Balance | Work-Life Balance | `overTime` (Yes/No) | The story text explicitly names overtime — not a judgment call |
| US-16 Career Progression | Career Progression | `yearsSinceLastPromotion`, bucketed into `0-2 years` / `3-5 years` / `6+ years` | "Career progression" isn't tied to a specific field; years-since-last-promotion is the closest match among the available years-based fields — a judgment call, bucketed since it's continuous |

**Why one shared `aggregateAttritionBy` helper instead of six separate implementations:** all six analyses are structurally the *same operation* — group a list by some key, count attrition within each group, compute a rate — differing only in *which* field (or bucketing function) produces the group key. Writing six near-identical loops would mean any bug fix or behavior change (e.g. how "Unknown" is handled, or switching `TreeMap` for a different ordering) would need to be made and tested six times instead of once. Each of the six public methods is therefore a one-line call passing a different private key-extractor function (`this::departmentOrUnknown`, `this::salaryBand`, etc.) into the shared helper.

**Why a `TreeMap` specifically:** it keeps group labels in sorted (alphabetical) order automatically, so results come back in a stable, predictable order rather than whatever order groups happened to be encountered in. Note this is plain string ordering, not numeric — for the compensation dimension, `"$100000-$149999"` sorts *before* `"$50000-$99999"` alphabetically (`"1" < "5"`), which is why the frontend's Employee List filter re-sorts compensation bands numerically itself (stripping non-digits and comparing as numbers) rather than trusting the backend's order for that one dimension.

## How `Attrition` (Yes/No) is used

The Survey API's `Attrition` field (mapped to `EmployeeDto.attrition()`, a `"Yes"`/`"No"` string) is the numerator for every one of the six analyses: within each group, `attritionCount` is `group.stream().filter(e -> "Yes".equals(e.attrition())).count()`. It is never surfaced as a per-employee "risk score" anywhere in this service — it's a raw field from the Survey API, aggregated per group, nothing more.

This matters as a design point worth stating explicitly in a presentation: this project does not do any prediction or scoring — "attrition" is a fact the Survey API already records for each employee (did they, in fact, leave), and every analysis here is a straightforward count/percentage over that existing fact, grouped different ways. There's no machine learning model, no computed "risk" anywhere in this codebase.

## Employee flagging → Kafka

`POST /employees/{id}/flag`:
1. Extracts the caller's email from the JWT (`EmployeeController.currentUserEmail`, via the local verify-only `JwtService`).
2. `EmployeeService.flagEmployee` re-uses `getEmployeeById` to fetch the employee's current name/department from the Survey API.
3. Builds an `EmployeeFlaggedEvent` (fresh `eventId`, employee snapshot, comment, caller's identity, timestamp) and publishes it via `EmployeeFlaggedEventProducer` — see [kafka.md](kafka.md) for the full flow, event schema, topic, and idempotency mechanism.
4. Returns `202 Accepted` with the event body once Kafka acknowledges the publish (the send is made synchronous with `.get()` specifically so a broker failure surfaces as an error here, not silently).

**Why `202 Accepted` and not `200 OK` or `201 Created`:** `202` specifically means "request accepted, processing will complete asynchronously" — which is exactly what's true here. The flag has been durably published to Kafka, but the actual `Notification` row doesn't exist yet at the moment this response is sent; it's created moments later by notification-service consuming the event. Returning `201 Created` would incorrectly imply the notification itself already exists as a resource at this point.

**Why the Kafka send is made synchronous (`.get()`) instead of fire-and-forget:** without it, a broker being down would let the HTTP request succeed while the event silently never gets published — the HR user would see a success response for a flag that produced no notification, with no way to know it failed. Calling `.get()` on the send future means this method blocks until Kafka acknowledges the write, so a broker failure becomes a thrown `EventPublicationException` here, converted to a clear `503` for the caller, rather than a silent data-loss bug.

**Caller identity:** `JwtService.extractEmail` reads the token's `email` claim (`claims.get("email", String.class)`) — not the `sub` claim, which is the user's UUID and is left untouched for anything that needs the stable identifier. `hrUserEmail` on events published here is therefore the flagging user's actual email address. (An earlier version of this method incorrectly returned `claims.getSubject()`; notification-service's identically-named method had the same defect and was fixed at the same time — see [notification-service.md](notification-service.md). Notifications created before the fix still hold the old UUID value in `notification_db`, since existing rows aren't retroactively migrated.)

## Docker

Standard backend Dockerfile (see [docker.md](docker.md)). `docker-compose.yml` sets `-Dsurvey-api.base-url=http://host.docker.internal:3232` (with `extra_hosts: host.docker.internal:host-gateway` so that resolves inside the container) and `-Dspring.kafka.bootstrap-servers=kafka:9092`. No database dependency, but waits on `kafka` being healthy before starting.

`host.docker.internal` is a special hostname Docker Desktop provides that resolves *back out* to the host machine from inside a container — needed here specifically because the Survey API isn't one of this project's own containers; it's assumed to be running on the host (or wherever the developer points it), so this service needs a way to reach outside the Docker network entirely, which no Eureka-resolved or Compose-service-name address could do.

## Testing

| Test | Covers |
|---|---|
| `EmployeeServiceApplicationTests` | Context loads |
| `EmployeeControllerTest` | Endpoint behavior (search validation, 404s, flag response codes) |
| `EmployeeMapperTest` | `SurveyEmployeeResponse` → `EmployeeDto` field mapping |
| `EmployeeServiceTest` | Attrition aggregation math for all six dimensions, flag → event publication |
| `SurveyApiClientIT` | **Not run by default** — named with the Failsafe `*IT` convention, but no Failsafe plugin is configured, so plain `mvn test` (Surefire, `*Test.java` pattern) skips it. Requires a live Survey API; run manually (e.g. point your IDE's test runner at the class) if you need it |

`EmployeeServiceTest` is the most important test class to be able to talk about — it's the one that actually verifies the attrition math (that a group with 3 attritions out of 12 employees reports a 25% rate, that bucket boundaries for salary/years land in the right band, that a blank/null field falls into "Unknown" rather than throwing or being silently dropped). `EmployeeMapperTest` separately guards the Survey API → `EmployeeDto` field-by-field translation, so a future Survey API field rename would be caught here rather than surfacing as a silently-null field in the UI.

## How to explain this service in a presentation

"Employee Service is the odd one out architecturally — it's the only service with no database. Employee data belongs to an external Survey API, so this service is a stateless proxy: every request, including all six attrition-analysis endpoints, calls the Survey API live and computes the answer on the spot, nothing is cached or stored here. It exposes the employee list, employee details, single-field search, and the six attrition dimensions — department, job role, compensation, demographics, work-life balance, and career progression — all built on one shared aggregation helper that groups employees by a field and computes an attrition rate per group. It also owns the flag endpoint, which doesn't create a notification directly — it publishes a Kafka event and returns 202 Accepted immediately, because the actual notification is created asynchronously by Notification Service. It has no Spring Security of its own; the Gateway is the only place access to this service's routes is actually enforced, since this service has no sensitive data of its own to defend at the persistence layer."
