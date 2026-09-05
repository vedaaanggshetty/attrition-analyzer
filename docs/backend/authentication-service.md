# Authentication Service

## Purpose

Owns identity: credentials (email + hashed password), roles, JWT issuance, and password reset. It does **not** own profile information (name, phone) — that's `user-profile-service`'s job, linked only by a shared `user_id` UUID.

## Architecture / packages

| Package | Contents |
|---|---|
| `controller` | `AuthController` (public-facing), `InternalCredentialController` (service-to-service only) |
| `service` | `AuthService` — all business logic |
| `entity` | `Credential`, `PasswordResetToken`, `Role` (enum, currently only `HR`) |
| `repository` | `CredentialRepository`, `PasswordResetTokenRepository` (Spring Data JPA) |
| `dto` | Request/response records for each endpoint |
| `security` | `JwtService` (signs **and** verifies — the only service that signs), `JwtAuthenticationFilter`, `SecurityConfig`, `AuthenticatedUser`, `TokenHashUtil` |
| `exception` | `DuplicateEmailException`, `InvalidCredentialsException`, `InvalidResetTokenException`, `GlobalExceptionHandler` |

## Controllers and endpoints

### `AuthController` (`/auth`) — public-facing

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/auth/login` | No | `{email, password}` → `{token, tokenType, expiresInMs}` |
| POST | `/auth/logout` | Yes | JWT is stateless — nothing to invalidate server-side. This endpoint exists only to satisfy US-03 and confirm the caller held a valid token at call time; the frontend doesn't actually call it, it just discards the token locally |
| POST | `/auth/reset-password/request` | No | Starts a reset; always returns the same generic message regardless of whether the email exists, so the endpoint never reveals which emails are registered |
| POST | `/auth/reset-password/confirm` | No | Completes a reset given a valid, unused, unexpired token |

### `InternalCredentialController` (`/internal/credentials`) — **internal only**

`POST /internal/credentials` creates a credential record and returns the generated `userId`. This is called by `user-profile-service` via Feign during registration — **it is not the frontend's registration endpoint**, and the Gateway has no route to it at all (see [api-gateway.md](api-gateway.md)), so it's unreachable except from another container on `attrition-net`.

**Security caveat, called out directly in `SecurityConfig`'s comments:** `/internal/**` is `permitAll()` because there's no service-to-service auth mechanism (mTLS, shared internal token, network policy) implemented yet — anyone who can reach this service's port directly (bypassing the Gateway) could currently call it. This is a documented, known gap, not an oversight.

## Business logic (`AuthService`)

- **Login:** look up by email, `BCrypt`-verify the password, sign a JWT with `sub`=userId, `email`, `role` claims via `JwtService.generateToken`.
- **Register credential:** reject if the email already exists (`existsByEmail`), otherwise generate a `UUID` **in application code** (not `@GeneratedValue`) so it can be returned to the caller (`user-profile-service`) before that caller saves its own row using the same UUID.
- **Password reset request:** generates a raw token, persists only its SHA-256 hash (`TokenHashUtil`) with a 15-minute expiry — the raw token is never logged or returned; actual email delivery is not implemented yet, so there's currently no way for a user to receive the raw token in this codebase.
- **Password reset confirm:** validates the token (exists, unused, unexpired) via the hash, updates the credential's password hash, marks the token used so it can't be replayed.

## Data model

`Credential` (table `credentials` in `authentication_db`): `user_id` (UUID, PK), `email` (unique), `password_hash` (BCrypt, via Spring Security's `BCryptPasswordEncoder`), `role` (`Role.HR` — Guest is intentionally **not** a stored value; it's the absence of a credential/token, not a row), `created_at`.

`PasswordResetToken`: token hash, `user_id`, `expires_at`, used flag.

See [database.md](database.md) for the full schema table and how this database now lives inside the single shared `mysql-db` container.

## Security

- **Signs** JWTs (the only service that does) — HMAC-SHA256 via `jjwt`, using the shared `JWT_SECRET`.
- Own `SecurityFilterChain`: `/auth/login`, `/auth/reset-password/**`, `/internal/**`, `/actuator/**` are public; everything else requires a valid JWT (defense-in-depth — the Gateway also enforces this).
- Passwords are always `BCrypt`-hashed; the raw password is never persisted, logged, or returned in any response.

## Communication with other services

- **Inbound only** from `user-profile-service` (Feign → `POST /internal/credentials`), resolved via Eureka.
- Never calls another service itself.

## Docker

Standard backend Dockerfile (see [docker.md](docker.md)). `docker-compose.yml` overrides its datasource URL to point at `mysql-db:3306/authentication_db` and waits for `mysql-db`'s healthcheck before starting.

## Testing

| Test | Covers |
|---|---|
| `AuthServiceApplicationTests` | Context loads |
| `AuthControllerTest` | Login/logout/reset endpoints |
| `InternalCredentialControllerTest` | Internal registration contract |
| `CredentialTest` | Entity behavior (`changePassword`, etc.) |
| `JwtServiceTest`, `JwtAuthenticationFilterTest`, `SecurityChainIntegrationTest` | JWT signing/verification, filter, route auth rules |
| `AuthServiceTest` | Unit tests against mocked repositories |
| `AuthServiceIntegrationTest` | Runs against an in-memory H2 database (`MODE=MySQL`) — doesn't need Docker/MySQL running |
