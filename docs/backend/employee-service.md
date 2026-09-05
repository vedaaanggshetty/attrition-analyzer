# Employee Service

## Purpose

Employee records, search, employee details, the six attrition-analysis dimensions (US-11 → US-16), and flagging an employee for HR follow-up (which kicks off the [Kafka flow](kafka.md)). This service gets particular attention because it's the one with the least conventional architecture in the project: **it has no database of its own.**

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

## How `Attrition` (Yes/No) is used

The Survey API's `Attrition` field (mapped to `EmployeeDto.attrition()`, a `"Yes"`/`"No"` string) is the numerator for every one of the six analyses: within each group, `attritionCount` is `group.stream().filter(e -> "Yes".equals(e.attrition())).count()`. It is never surfaced as a per-employee "risk score" anywhere in this service — it's a raw field from the Survey API, aggregated per group, nothing more.

## Employee flagging → Kafka

`POST /employees/{id}/flag`:
1. Extracts the caller's email from the JWT (`EmployeeController.currentUserEmail`, via the local verify-only `JwtService`).
2. `EmployeeService.flagEmployee` re-uses `getEmployeeById` to fetch the employee's current name/department from the Survey API.
3. Builds an `EmployeeFlaggedEvent` (fresh `eventId`, employee snapshot, comment, caller's identity, timestamp) and publishes it via `EmployeeFlaggedEventProducer` — see [kafka.md](kafka.md) for the full flow, event schema, topic, and idempotency mechanism.
4. Returns `202 Accepted` with the event body once Kafka acknowledges the publish (the send is made synchronous with `.get()` specifically so a broker failure surfaces as an error here, not silently).

**Caller identity:** `JwtService.extractEmail` reads the token's `email` claim (`claims.get("email", String.class)`) — not the `sub` claim, which is the user's UUID and is left untouched for anything that needs the stable identifier. `hrUserEmail` on events published here is therefore the flagging user's actual email address. (An earlier version of this method incorrectly returned `claims.getSubject()`; notification-service's identically-named method had the same defect and was fixed at the same time — see [notification-service.md](notification-service.md). Notifications created before the fix still hold the old UUID value in `notification_db`, since existing rows aren't retroactively migrated.)

## Docker

Standard backend Dockerfile (see [docker.md](docker.md)). `docker-compose.yml` sets `-Dsurvey-api.base-url=http://host.docker.internal:3232` (with `extra_hosts: host.docker.internal:host-gateway` so that resolves inside the container) and `-Dspring.kafka.bootstrap-servers=kafka:9092`. No database dependency, but waits on `kafka` being healthy before starting.

## Testing

| Test | Covers |
|---|---|
| `EmployeeServiceApplicationTests` | Context loads |
| `EmployeeControllerTest` | Endpoint behavior (search validation, 404s, flag response codes) |
| `EmployeeMapperTest` | `SurveyEmployeeResponse` → `EmployeeDto` field mapping |
| `EmployeeServiceTest` | Attrition aggregation math for all six dimensions, flag → event publication |
| `SurveyApiClientIT` | **Not run by default** — named with the Failsafe `*IT` convention, but no Failsafe plugin is configured, so plain `mvn test` (Surefire, `*Test.java` pattern) skips it. Requires a live Survey API; run manually (e.g. point your IDE's test runner at the class) if you need it |
