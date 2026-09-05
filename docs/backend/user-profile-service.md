# User Profile Service

## Purpose

Owns HR profile data (full name, email, phone) and the public registration flow. Coordinates with Authentication Service to create the credential half of a new account, but exposes only a profile-facing API to the outside world.

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

## `AuthenticationClient` (Feign)

```java
@FeignClient(name = "authentication-service")
public interface AuthenticationClient {
    @PostMapping("/internal/credentials")
    AuthCredentialResponse registerCredential(@RequestBody AuthCredentialRequest request);
}
```

Resolved via Eureka using authentication-service's `spring.application.name` — no hard-coded host/port. This call goes directly container-to-container, **not** through the API Gateway (there's no Gateway route to `/internal/**` at all — see [api-gateway.md](api-gateway.md)).

## Data model

`UserProfile` (table `profiles` in `user_profile_db`): `user_id` (UUID, PK — the **same** UUID Authentication generated, never generated here), `full_name`, `email` (unique), `phone`, `created_at`, `updated_at`. See [database.md](database.md).

## Security

Own `SecurityFilterChain`: `/users/register` is public, everything else (`/users/me`) requires a valid JWT — same verify-only `JwtService`/`JwtAuthenticationFilter` pattern as every other service that validates tokens (this service never signs one).

## Communication with other services

- **Outbound:** Feign → authentication-service, `POST /internal/credentials`, during registration only.
- **Inbound:** reached through the Gateway (`/users/**`) for both public and authenticated endpoints.

## Docker

Standard backend Dockerfile (see [docker.md](docker.md)). `docker-compose.yml` overrides its datasource URL to point at `mysql-db:3306/user_profile_db` and waits for `mysql-db`'s healthcheck before starting.

## Testing

| Test | Covers |
|---|---|
| `UserProfileServiceApplicationTests` | Context loads |
| `ProfileControllerTest`, `UserRegistrationControllerTest` | Controller-level behavior |
| `JwtServiceTest`, `JwtAuthenticationFilterTest`, `SecurityChainIntegrationTest` | JWT verification, filter, route auth rules |
| `ProfileServiceTest`, `UserRegistrationServiceTest` | Unit tests against mocked repositories/Feign client |
| `ProfileServiceIntegrationTest`, `UserRegistrationServiceIntegrationTest` | Run against an in-memory H2 database (`MODE=MySQL`) — no Docker/MySQL required |
