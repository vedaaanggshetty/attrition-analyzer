# Swagger / OpenAPI and Health Checks

This documents what was added on top of the existing backend: OpenAPI documentation for every
business service, and an explicit, minimal Actuator health-check configuration. Nothing in the
existing architecture, security configuration, or routing changed - this only adds documentation
and a health endpoint to services that already existed.

## Which services

Applies to the four business services - **authentication-service**, **user-profile-service**,
**employee-service**, **notification-service**. `discovery-service` (Eureka's own dashboard, not a
REST API to document) and `api-gateway` (a router, not a set of business endpoints) are
intentionally excluded, same as they're excluded from the per-service test requirement.

## Swagger / OpenAPI

**Dependency**: `springdoc-openapi-starter-webmvc-ui:3.0.0` - the version confirmed to resolve and
run cleanly against this project's Spring Boot 4.0.8 / Spring Framework 7 stack.

**Where to look**:

| What | URL (per service's own port) |
|---|---|
| Swagger UI | `http://localhost:<port>/swagger-ui.html` |
| Raw OpenAPI spec | `http://localhost:<port>/v3/api-docs` |

| Service | Port |
|---|---|
| authentication-service | 8081 |
| user-profile-service | 8082 |
| employee-service | 8083 |
| notification-service | 8084 |

Swagger UI is served directly by each service on its own port - it is not proxied through the
Gateway (the Gateway routes business paths like `/auth/**`, not `/swagger-ui.html`), so it's a
local/direct-access developer tool, the same way `/actuator/health` is.

**Annotations used, and why**:

| Annotation | Where | Why |
|---|---|---|
| `@Tag` | Once per controller class | Groups that controller's endpoints under one heading in Swagger UI (e.g. "Authentication", "Notifications") instead of one flat list. |
| `@Operation` | Once per endpoint method | A one-line `summary` (always) plus a short `description` when the behavior isn't obvious from the summary and the URL alone - e.g. that flagging an employee returns before the notification actually exists. |
| `@ApiResponses` / `@ApiResponse` | Once per endpoint method | Documents the real HTTP status codes that endpoint can return, each with what it means - taken directly from each service's `GlobalExceptionHandler`, never invented. |
| `@Parameter` | Path/query parameters | Explains what a path variable or query param means (e.g. employee-service's `property`/`value` search params); the `Authorization` header parameter is marked `hidden = true` since `@SecurityRequirement` already documents auth once per endpoint. |
| `@RequestBody` (`io.swagger...parameters.RequestBody`, not Spring's) | POST/PUT/PATCH bodies | A short note on what the body is for, alongside the `@Schema` on the DTO itself. |
| `@Schema` | Every request/response DTO and its fields | A short description per field, and an example value for anything non-obvious (an enum-like string, a UUID, a date format). |
| `@SecurityRequirement(name = "bearerAuth")` | Controller class (default) or overridden per-endpoint | Marks which endpoints need a JWT - matched against each service's real `SecurityConfig`/Gateway rule, not assumed. |

**Key differences between services' Swagger setup** (all driven by real differences in how each
service is actually secured, not arbitrary):

- **authentication-service** and **user-profile-service** have their own Spring Security filter
  chain, so their `@Tag`s sit on controllers that mix public and protected endpoints in the same
  class (`AuthController` has both public `/auth/login` and protected `/auth/logout`) - security is
  annotated per-endpoint there, not once at the class level.
- **employee-service** and **notification-service** have *no* Spring Security dependency at all -
  they rely entirely on the Gateway for authorization and only read the JWT's email claim directly
  for business logic. `NotificationController` puts `@SecurityRequirement(name = "bearerAuth")`
  once at the class level since every one of its endpoints requires a session; `EmployeeController`
  annotates per-endpoint instead, since six of its endpoints are genuinely public
  (`/employees/analysis/**`, Guest-visible) alongside protected ones.
- **authentication-service** additionally has an internal-only tag (`Internal Credentials`) for
  `POST /internal/credentials` - documented as requiring no token (correct: it's a trusted
  Feign-only call, never reachable through the Gateway) but explicitly not meant to be called from
  a browser, which the `@Tag` description says outright.

**What was added, per service**:

- An `OpenApiConfig` class (`config` package) registering the API title/description and a
  `bearerAuth` HTTP-bearer security scheme, so "Authorize" in Swagger UI actually works against
  real endpoints.
- `@Tag` on every controller, grouping endpoints in the UI (e.g. "Authentication", "Profile",
  "Employees", "Notifications").
- `@Operation` on every endpoint with a one-line `summary` and, where the behavior isn't obvious
  from the summary alone, a short `description`.
- `@ApiResponses`/`@ApiResponse` listing the real HTTP status codes each endpoint can actually
  return (200/201/202/204, 400, 401, 404, 409, 503 as applicable) - taken directly from each
  service's `GlobalExceptionHandler`, not invented.
- `@Parameter` on path/query parameters (e.g. `id` path variables, `property`/`value` query
  params on employee search); the `Authorization` header parameter is marked `hidden = true`
  since it's already documented once at the class level via `@SecurityRequirement`.
- `@RequestBody` (the Swagger annotation, not Spring's) describing what each request body is for.
- `@Schema` on every request/response DTO and its fields, with a short description and, for
  non-obvious fields, an example value.
- `@SecurityRequirement(name = "bearerAuth")` on every endpoint that actually requires a JWT
  (matching each service's real security posture - see below), so Swagger UI correctly shows
  which endpoints need "Authorize" and which don't.

**Endpoints correctly marked as not requiring a token** (because they genuinely don't, verified
against each service's `SecurityConfig`/Gateway routing, not assumed):

- `POST /auth/login` (authentication-service)
- `POST /internal/credentials` (authentication-service - internal, Feign-only)
- `POST /users/register` (user-profile-service)
- `GET /employees/analysis/**` (employee-service - the six Guest-visible attrition endpoints)

Every other endpoint is documented as requiring `bearerAuth`, consistent with the Gateway's
`anyRequest().authenticated()` default and each service's own defense-in-depth checks.

**What was deliberately not annotated**: private helper methods (e.g.
`EmployeeController.currentUserEmail`), internal service/repository classes, and Kafka
listener/producer classes - none of these are REST endpoints, so annotating them would just add
noise without documenting anything Swagger UI can show.

## Actuator / health checks

Every business service already had `spring-boot-starter-actuator` on the classpath (used by the
existing `docker-compose.yml` health checks). What changed is making the exposure explicit rather
than relying on Spring Boot's default:

```properties
# Only expose health over HTTP - no env/beans/heapdump/threaddump/etc, and no
# internal detail (DB status, disk space) leaked to an unauthenticated caller.
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=never
```

This is the same block, verbatim, in all four services' `application.properties`.

- **`GET /actuator/health`** is public on every service (already permitted in each service's
  `SecurityConfig` before this change - not modified here) and returns `{"status":"UP"}` once the
  service and its dependencies (DB connection pool, Kafka client where applicable) are ready.
- No other Actuator endpoint is exposed - `show-details=never` also means an unauthenticated
  caller never sees *why* a dependency is down, only that the service overall is up or not.
- This works identically in Docker and locally: it's a plain HTTP endpoint with no
  environment-specific branching, and `docker-compose.yml`'s existing health checks
  (`mysql-db`, `kafka`) were already following this same "wait for a real health signal, not just
  process-started" pattern - this extends that same idea to the four business services'
  own health, verifiable directly via `curl http://localhost:<port>/actuator/health`.

## Verification performed

- `mvn test` passes for all four services after these changes (existing tests untouched by this
  work continue to pass; no test asserted on the *absence* of Swagger/Actuator config).
- `mvn package` produces a working jar for all four services.
- Rebuilt and redeployed all four containers via `docker compose up -d --build`, then verified
  live: `/actuator/health` returns `200 {"status":"UP"}` and `/v3/api-docs` returns `200` with a
  valid OpenAPI document (containing the expected tags and paths) on all four service ports;
  `/swagger-ui.html` redirects (302, springdoc's normal behavior) to a working UI (200 after
  following the redirect).
- Confirmed the Gateway, Eureka, and the frontend were unaffected (`/actuator/health` on the
  Gateway, Eureka's dashboard, and the frontend's root page all still return 200).

## Tests

See [testing.md](testing.md) for the full breakdown of every test class, per service, what it
actually proves, and unit vs. integration coverage.
