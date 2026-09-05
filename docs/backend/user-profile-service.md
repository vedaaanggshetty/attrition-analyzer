# User Profile Service

## Purpose

Owns HR profile data (full name, email, phone) and the public registration flow. Coordinates with Authentication Service to create the credential half of a new account, but exposes only a profile-facing API to the outside world.

**Why registration lives here and not on Authentication Service:** registration is fundamentally a *profile* creation action from the outside world's point of view (a new HR user is signing up with a name and contact info), even though it also needs a credential created underneath. Putting the public `/users/register` endpoint here, and having it privately delegate the credential half to Authentication, keeps Authentication's public surface minimal (it only ever exposes login/logout/password-reset, never "create a new identity" directly) and keeps the profile-facing contract (what the frontend actually sends/receives on signup) owned by the service that owns that data shape.

**What this service owns:** the `profiles` table, and the orchestration logic that turns one registration request into a credential (via Authentication) plus a profile (locally). **What it explicitly does not own:** password hashing/storage, JWT issuance, or login — a `PUT /users/me` call, for example, can never change a user's password or email/login identity, only their name/phone.

## Architecture / packages

| Package | Contents |
|---|---|
| `controller` | `UserRegistrationController` (public), `ProfileController` (authenticated "my profile") |
| `service` | `UserRegistrationService`, `ProfileService` |
| `entity` | `UserProfile` |
| `repository` | `UserProfileRepository` |
| `client` | `AuthenticationClient` — Feign client to authentication-service |
| `dto` | Request/response records, including the Feign contract's own DTOs (`AuthCredentialRequest`/`Response`) |
| `security` | Own copy of `JwtService`, `JwtAuthenticationFilter`, `SecurityConfig`, `AuthenticatedUser` |
| `exception` | `DuplicateEmailException`, `ProfileNotFoundException`, `AuthenticationServiceException`, `GlobalExceptionHandler` |

The `client` package is the one addition to the usual controller/service/repository shape (compared to authentication-service) — it exists specifically because this service is the *caller* in one cross-service relationship (Feign to authentication-service), whereas authentication-service is purely a *callee* everywhere it's involved. `AuthenticationServiceException` exists as its own exception type (rather than just letting a `FeignException` propagate) so that a failure talking to Authentication surfaces to the frontend as a clear, service-specific error message instead of a raw Feign/HTTP exception leaking through.

## Controllers and endpoints

### `UserRegistrationController` (`/users`) — public

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/users/register` | No | `{fullName, email, password, phone?}` → `201` with the new profile |

### `ProfileController` (`/users`) — authenticated

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/users/me` | Yes | Returns the **caller's own** profile, identified from the JWT principal |
| PUT | `/users/me` | Yes | `{fullName, phone}` — updates the caller's own profile; email is not updatable here |

Both `/me` endpoints take the target user from `@AuthenticationPrincipal AuthenticatedUser principal`, never from a client-supplied ID — there is no way to view or edit anyone else's profile through this API.

**`GET /users/me`, end to end:**

```
Frontend → GET /users/me  (Authorization: Bearer <token>)
  → Gateway: verifies JWT, populates AuthenticatedUser, routes to user-profile-service
    → this service's OWN JwtAuthenticationFilter verifies the token AGAIN (defense-in-depth)
      and populates its own AuthenticatedUser principal
      → ProfileController.getMyProfile(@AuthenticationPrincipal principal)
        → ProfileService.getProfile(principal.userId())
          → UserProfileRepository.findById(userId) — not found → ProfileNotFoundException (404)
  ← 200 ProfileResponse {userId, fullName, email, phone, createdAt, updatedAt}
```

The `userId` used for the lookup comes entirely from the verified JWT's `sub` claim — never from a query parameter or path variable — which is what makes "view/edit anyone else's profile" structurally impossible through this API, not just something the frontend happens not to expose.

## Registration flow (`UserRegistrationService`)

```
Frontend
  │  POST /users/register {fullName, email, password, phone}
  ▼
User Profile Service
  │  1. existsByEmail(email)? → reject early (no orphan credential created)
  │  2. Feign → POST /internal/credentials {email, password}  (authentication-service)
  │       → creates the Credential row, returns the generated userId
  │  3. save UserProfile(userId, fullName, email, phone) locally
  ▼
201 Created — RegisterUserResponse
```

This two-step coordination is why the fail-fast local check exists first: without it, a duplicate email could create a valid credential in Authentication before failing to save the profile locally, leaving an orphaned credential with no matching profile. `FeignException.Conflict` (if Authentication itself detects a duplicate in a race) is also mapped to the same `DuplicateEmailException`; any other Feign failure becomes an `AuthenticationServiceException`.

The password is **never persisted in this service's own database** — it only ever passes through this service on its way to Authentication via the Feign call.

**What happens on each kind of failure during registration:**

- Email already has a local profile → `DuplicateEmailException` (`409`) thrown immediately, before any Feign call — no credential is created at all.
- Email is free locally, but Authentication reports a conflict anyway (a race, or data drifted out of sync between the two databases) → the Feign call throws `FeignException.Conflict`, caught and re-thrown as the same `DuplicateEmailException` (`409`) — the caller sees one consistent error type regardless of which side detected the duplicate.
- Authentication is unreachable, times out, or returns any other error → any other `FeignException` is wrapped as `AuthenticationServiceException`, mapped by `GlobalExceptionHandler` to `503 Service Unavailable` — clearly distinct from "this email is taken" (`409`), so the frontend can tell a user "try again later" instead of "pick a different email."

## `AuthenticationClient` (Feign)

```java
@FeignClient(name = "authentication-service")
public interface AuthenticationClient {
    @PostMapping("/internal/credentials")
    AuthCredentialResponse registerCredential(@RequestBody AuthCredentialRequest request);
}
```

Resolved via Eureka using authentication-service's `spring.application.name` — no hard-coded host/port. This call goes directly container-to-container, **not** through the API Gateway (there's no Gateway route to `/internal/**` at all — see [api-gateway.md](api-gateway.md)).

Declaring a Feign client is intentionally close to writing a regular interface — Spring Cloud OpenFeign generates the actual HTTP client implementation at runtime from the `@FeignClient`/`@PostMapping` annotations. This is why the code reads like a normal method call (`authenticationClient.registerCredential(request)`) even though it's really an HTTP `POST` to another container; the alternative (`RestTemplate`/`WebClient` calls built by hand, with manual URL construction and JSON (de)serialization) is more code for the same result, without giving up anything this project needs. This is also the **only** Feign client in the project — every other cross-service interaction is either Gateway-routed REST from the frontend, or the Kafka flow (see [kafka.md](kafka.md)) — because it's the only case where one internal service needs to call another synchronously and expects an answer before continuing its own request.

## Data model

`UserProfile` (table `profiles` in `user_profile_db`): `user_id` (UUID, PK — the **same** UUID Authentication generated, never generated here), `full_name`, `email` (unique), `phone`, `created_at`, `updated_at`. See [database.md](database.md).

Storing `email` here too (duplicated from `credentials.email` in a different database) is a deliberate trade-off, not an oversight: it lets this service answer "what's this user's email" and enforce its own uniqueness check (`existsByEmail`, used for the fail-fast registration check) without calling Authentication for every read. The cost is that the two copies could in principle drift if one were updated without the other — which is exactly why `PUT /users/me` doesn't allow changing email at all (see below), avoiding that drift entirely rather than trying to keep two databases in sync.

## Security

Own `SecurityFilterChain`: `/users/register` is public, everything else (`/users/me`) requires a valid JWT — same verify-only `JwtService`/`JwtAuthenticationFilter` pattern as every other service that validates tokens (this service never signs one).

Just like authentication-service, this is a second, independent check on top of the Gateway's — same defense-in-depth reasoning: if this service's port were reached directly (bypassing the Gateway), `/users/me` would still be protected by its own filter chain, not left open.

## Communication with other services

- **Outbound:** Feign → authentication-service, `POST /internal/credentials`, during registration only.
- **Inbound:** reached through the Gateway (`/users/**`) for both public and authenticated endpoints.

Unlike authentication-service (a pure "leaf" with no outbound calls), this service sits in the *middle* of the registration flow — it receives a request from the frontend and, before it can finish handling it, makes its own request onward to another service. This is the one place in the whole backend where a single incoming HTTP request triggers a second, synchronous HTTP request to a different service before a response goes back to the caller.

## Docker

Standard backend Dockerfile (see [docker.md](docker.md)). `docker-compose.yml` overrides its datasource URL to point at `mysql-db:3306/user_profile_db` and waits for `mysql-db`'s healthcheck before starting.

This service does *not* explicitly `depends_on` authentication-service in `docker-compose.yml` — it only waits on `mysql-db`. That's fine at container-startup time (Eureka registration happens independently and this service can start before Authentication is ready), but it does mean the very first registration attempt could hit a Feign call before Authentication has finished registering with Eureka; that surfaces as the same `AuthenticationServiceException`/`503` described above, and simply retrying moments later succeeds once Authentication is up.

## Testing

| Test | Covers |
|---|---|
| `UserProfileServiceApplicationTests` | Context loads |
| `ProfileControllerTest`, `UserRegistrationControllerTest` | Controller-level behavior |
| `JwtServiceTest`, `JwtAuthenticationFilterTest`, `SecurityChainIntegrationTest` | JWT verification, filter, route auth rules |
| `ProfileServiceTest`, `UserRegistrationServiceTest` | Unit tests against mocked repositories/Feign client |
| `ProfileServiceIntegrationTest`, `UserRegistrationServiceIntegrationTest` | Run against an in-memory H2 database (`MODE=MySQL`) — no Docker/MySQL required |

`UserRegistrationServiceTest` (mocked `AuthenticationClient`) is what actually proves the three failure branches described above — the duplicate-locally, duplicate-remotely, and Authentication-unreachable cases — without needing a real authentication-service running; `UserRegistrationServiceIntegrationTest` then proves the same flow against a real (in-memory) database, confirming the profile row is actually persisted with the right `user_id` once the mocked Feign call "succeeds."

## How to explain this service in a presentation

"User Profile Service owns HR profile data — name, email, phone — and the public registration endpoint. It doesn't own passwords or issue tokens at all; when someone registers, this service first checks locally that the email isn't already taken, then calls Authentication Service internally, through Feign, to create the credential and get back a generated user ID, and only then saves its own profile row using that same ID. That ordering matters — checking locally first avoids creating an orphaned credential if the profile save were to fail. The `/users/me` endpoints always act on the caller's own profile, identified from their JWT, never from a client-supplied ID, so there's no way to view or edit someone else's profile through this API. And just like Authentication Service, it independently re-validates the JWT itself rather than only trusting the Gateway."
