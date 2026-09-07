# Swagger / OpenAPI and Health Checks

This documents what was added on top of the existing backend: OpenAPI documentation for every
business service, and an explicit, minimal Actuator health-check configuration. Nothing in the
existing architecture, security configuration, or routing changed - this only adds documentation
and a health endpoint to services that already existed.

## Which services

Full per-endpoint OpenAPI annotation (`@Tag`/`@Operation`/`@ApiResponses`/`@Schema`/etc., see
below) applies to the four business services - **authentication-service**,
**user-profile-service**, **employee-service**, **notification-service**. `discovery-service`
(Eureka's own dashboard, not a REST API to document) has no Swagger of its own.

**api-gateway** gets a different, simpler treatment - it has no business endpoints of its own to
annotate (it only routes to the four services above), so instead of documenting itself it hosts
**one unified Swagger UI, at `http://localhost:8080/swagger-ui.html`, that merges all four
services' endpoints into a single page** - grouped by tag (Authentication, User Profile, Employee,
Notifications), with real schemas, parameters, and status codes for every endpoint, and no
dropdown or separate pages to open. See "Unified Swagger UI on the Gateway" below.

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

## Unified Swagger UI on the Gateway

`api-gateway` also gets `springdoc-openapi-starter-webmvc-ui`, but for a different reason than the
four business services: it has no endpoints of its own to document. Instead of a dropdown that
sends you to four separate pages, `AggregatedOpenApiCustomizer`
(`api-gateway/src/main/java/com/example/APIGateway/config/AggregatedOpenApiCustomizer.java`)
**merges** all four services' OpenAPI documents into the Gateway's own, so
`http://localhost:8080/swagger-ui.html` is genuinely one page with every endpoint on it.

**How the merge works**, on every request to the Gateway's `/v3/api-docs` (caching is disabled for
this doc specifically - see below - so it's always current):

1. A load-balanced `RestTemplate` (`RestTemplateConfig`) fetches each service's own
   `http://<service-name>/v3/api-docs` via Eureka - the same discovery mechanism the Gateway's
   `lb://` routes already use, not a new one.
2. Each service's schema names are prefixed (`ErrorResponse` -> `AuthErrorResponse`,
   `ProfileErrorResponse`, `EmployeeErrorResponse`, `NotificationErrorResponse`, etc.) and every
   `$ref` pointing at them is rewritten to match. This is necessary, not cosmetic: every service
   independently defines its own `ErrorResponse`, `LoginRequest`-shaped DTOs, and so on - merged
   without renaming, the last one processed would silently overwrite the others' schema
   definitions in the combined document.
3. Each service's tags are renamed to the one group name it should appear under - e.g.
   user-profile-service's separate `Registration` and `Profile` tags both become **User Profile**,
   so the sidebar shows exactly the four groups requested: **Authentication, User Profile,
   Employee, Notifications**.
4. Paths, schemas, tags, and the shared `bearerAuth` security scheme are copied into the Gateway's
   document - **except `/internal/**`**, which is deliberately excluded (see the `Internal
   Credentials` note above): the Gateway has no route for it and never proxies it, so documenting
   it here would show an endpoint that isn't actually reachable through this page's own
   `servers: ["/"]`. Any tag left with no remaining path after that exclusion is dropped too, so it
   never appears as an empty group.
5. If a service is unreachable when the doc is being built, that one is skipped (logged as a
   warning) rather than failing the whole page - a service that's still starting up just means its
   endpoints are missing until the next request rebuilds the doc, not a broken Swagger page.

**Paths and schemas are the real thing, not re-typed**: because each service's own controllers and
DTOs are still the single source of truth (per-endpoint `@Tag`/`@Operation`/`@Schema`/etc.
annotated directly on them, as described above), the merge only copies already-generated JSON -
there's no separate, hand-maintained "gateway view" of the API that could drift from the actual
code.

`springdoc.cache.disabled=true` in `api-gateway`'s `application.properties` means this fetch-and-merge
happens on every load of the docs page (a handful of small internal HTTP calls, not real API
traffic) rather than once and cached - trading a little bit of latency on that one page for the
guarantee that it's never stale from a service that was down the first time someone opened it.

Each service's own Swagger UI (`http://localhost:<port>/swagger-ui.html`, ports 8081-8084) still
works exactly as before and is unaffected by this - useful for testing a service in isolation, or
for `/internal/credentials`, which (correctly) only shows up there, never on the Gateway's page.

### Server address and "Try it out"

The merged doc's `servers` is a single, explicit `http://localhost:8080` (not a relative `/`) -
set in `api-gateway`'s `OpenApiConfig`. "Try it out" always calls the Gateway directly, the same
address the frontend uses; it never calls an individual service's own port (8081-8084), even
though the underlying spec for each endpoint originally came from there.

### Access-level labels (Public / Guest-accessible / Requires Bearer JWT)

Every operation's description is prefixed with one of three labels, computed automatically in
`AggregatedOpenApiCustomizer.markAccessLevels` from the operation's actual (post-merge) `security`
requirement - never hand-typed, so the label can't drift from what's actually enforced:

- **Public - no token required.** - e.g. `POST /users/register`, `POST /auth/login`.
- **Guest-accessible - no token required.** - the six `GET /employees/analysis/**` endpoints,
  called out separately from plain "Public" since they're specifically what a logged-out Guest can
  see on the landing page (same enforcement as Public, distinguished only because the frontend
  treats them as a distinct surface).
- **Requires Bearer JWT (HR-only).** - everything else.

**A real bug this caught**: four of the six attrition-analysis endpoints
(`/employees/analysis/compensation`, `demographics`, `work-life-balance`, `career-progression`)
had been annotated with `@SecurityRequirement(name = "bearerAuth")` at the operation level, making
Swagger UI show them as requiring a token - but the Gateway's actual rule,
`.requestMatchers(HttpMethod.GET, "/employees/analysis/**").permitAll()`, permits all six without
one. Verified directly (`curl` with no `Authorization` header returned `200` for all four before
touching the docs), then fixed by removing the incorrect annotation from
`EmployeeController` - now all six are documented identically as Guest-accessible, matching what
the Gateway actually does.

### The documented flow

The Gateway doc's top-level description spells out the only order that makes sense given the
architecture (a Guest becomes an HR user by registering, logging in produces the JWT everything
else needs):

1. `POST /users/register` - create an HR account (Public)
2. `POST /auth/login` - log in, copy the `token` field from the response (Public)
3. Click **Authorize** (top right of Swagger UI) and paste the token
4. Call any protected endpoint - `GET /users/me`, `GET /employees`, `GET /employees/{id}`,
   `POST /employees/{id}/flag`, or any `/notifications/**` endpoint

Verified this exact sequence end-to-end against the live stack (register -> login -> call
`/users/me`, `/employees`, and `/notifications` with the returned token) - all succeeded.

**A second real bug this caught (the opposite direction of the one above)**: `GET /employees` and
`GET /employees/{id}` had **no `@SecurityRequirement` at all** - not because they're public, but
because they were simply never annotated with one. Swagger UI only attaches the Bearer token to an
operation that actually *declares* a security requirement in its spec; with none declared, "Try it
out" sent these two requests with no `Authorization` header even after clicking **Authorize**,
producing a `401` that looked like a broken integration. Verified directly (`curl` with no header
returned `401` for both, matching the Gateway's real `anyRequest().authenticated()` default - only
`/employees/analysis/**` is actually permitted without a token), then fixed by adding
`security = @SecurityRequirement(name = "bearerAuth")` to both operations in `EmployeeController`.
Confirmed after the fix: both endpoints correctly document `security` and `401` as a possible
response, and the full Register -> Login -> Authorize -> `GET /employees` -> `GET /employees/{id}`
-> `GET /employees/analysis/**` -> `POST /employees/{id}/flag` sequence succeeds end-to-end through
the Gateway's Swagger UI.

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

This is the same block, verbatim, in all four business services' `application.properties`, and was
added to `api-gateway`'s as well while adding its Swagger UI (the Gateway already exposed
`/actuator/health` before this change - only the explicit exposure/detail properties are new).

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
- Verified the Gateway's merged `/v3/api-docs` directly: exactly the four requested tags appear
  (`Internal Credentials` correctly excluded, along with its one path), all sixteen frontend-facing
  paths from all four services are present, and each service's `ErrorResponse` (and other
  same-named DTOs) landed under distinct prefixed schema names with `$ref`s pointing at the correct
  one - confirmed by inspecting `/auth/login`'s request body schema resolving to
  `AuthLoginRequest`, not a same-named schema from a different service.

## Tests

See [testing.md](testing.md) for the full breakdown of every test class, per service, what it
actually proves, and unit vs. integration coverage.
