# Authentication Service

## Purpose

Owns identity: credentials (email + hashed password), roles, JWT issuance, and password reset. It does **not** own profile information (name, phone) — that's `user-profile-service`'s job, linked only by a shared `user_id` UUID.

**Why identity and profile are split into two services instead of one "users" service:** they change for different reasons and have different sensitivity. Credentials (password hashes, tokens) need tight access control and rarely change; profile fields (name, phone) are edited far more often and have no security sensitivity. Splitting them means a bug or a future feature in profile editing can never accidentally touch how login/password-hashing works, and it mirrors a very common real-world pattern (identity provider vs. user-profile store) that this project deliberately follows at a smaller scale. The shared `user_id` UUID is the only thing connecting the two — Authentication doesn't know a user's name, and User Profile doesn't know their password hash.

**What this service owns:** credential storage (`credentials` table), password hashing/verification, JWT signing, and the password-reset token lifecycle. **What it explicitly does not own:** full name, phone, or any other profile detail (user-profile-service's job — see [user-profile-service.md](user-profile-service.md)); it also does not decide *authorization* (which routes a role can access) — that's enforced at the Gateway (see [api-gateway.md](api-gateway.md)) and, redundantly, in this service's own `SecurityConfig`.

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

The package split follows a standard layered shape (controller → service → repository, with entities and DTOs kept separate) used consistently across every backend service in this project — a deliberate convention, not something unique to this service. `GlobalExceptionHandler` (a `@RestControllerAdvice`) is what turns exceptions into the correct HTTP status/JSON body — controllers and `AuthService` never build an `ErrorResponse` by hand, they just throw, and this one class centralizes the mapping: `InvalidCredentialsException` → `401`, `DuplicateEmailException` → `409 Conflict`, `InvalidResetTokenException` → `400 Bad Request`, and Bean Validation failures (`@Valid` on a request DTO, e.g. a malformed email or missing password) → `400` with the first field error's message. Every error response follows the same `ErrorResponse` shape (`timestamp`, `status`, `error`, `message`), which is why the frontend's `apiClient.ts` can handle every backend error generically instead of special-casing each service.

## Controllers and endpoints

### `AuthController` (`/auth`) — public-facing

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/auth/login` | No | `{email, password}` → `{token, tokenType, expiresInMs}` |
| POST | `/auth/logout` | Yes | JWT is stateless — nothing to invalidate server-side. This endpoint exists only to satisfy US-03 and confirm the caller held a valid token at call time; the frontend doesn't actually call it, it just discards the token locally |
| POST | `/auth/reset-password/request` | No | Starts a reset; always returns the same generic message regardless of whether the email exists, so the endpoint never reveals which emails are registered |
| POST | `/auth/reset-password/confirm` | No | Completes a reset given a valid, unused, unexpired token |

**Login, end to end:**

```
Frontend → POST /auth/login {email, password}
  → Gateway (route matches /auth/**, permitAll — no token needed to log in) → authentication-service
    → AuthController.login → AuthService.login
      → CredentialRepository.findByEmail(email)  — not found → InvalidCredentialsException (401)
      → passwordEncoder.matches(rawPassword, storedHash) — mismatch → InvalidCredentialsException (401)
      → JwtService.generateToken(userId, email, role) — signs sub/email/role/iat/exp
  ← 200 { token, tokenType: "Bearer", expiresInMs }
```

Note that a wrong email and a wrong password produce the **same** `InvalidCredentialsException`/`401` — the service deliberately doesn't distinguish "no such user" from "wrong password" in its response, so a caller can't use the error to enumerate registered emails.

### `InternalCredentialController` (`/internal/credentials`) — **internal only**

`POST /internal/credentials` creates a credential record and returns the generated `userId`. This is called by `user-profile-service` via Feign during registration — **it is not the frontend's registration endpoint**, and the Gateway has no route to it at all (see [api-gateway.md](api-gateway.md)), so it's unreachable except from another container on `attrition-net`.

**Security caveat, called out directly in `SecurityConfig`'s comments:** `/internal/**` is `permitAll()` because there's no service-to-service auth mechanism (mTLS, shared internal token, network policy) implemented yet — anyone who can reach this service's port directly (bypassing the Gateway) could currently call it. This is a documented, known gap, not an oversight.

## Business logic (`AuthService`)

- **Login:** look up by email, `BCrypt`-verify the password, sign a JWT with `sub`=userId, `email`, `role` claims via `JwtService.generateToken`.
- **Register credential:** reject if the email already exists (`existsByEmail`), otherwise generate a `UUID` **in application code** (not `@GeneratedValue`) so it can be returned to the caller (`user-profile-service`) before that caller saves its own row using the same UUID.
- **Password reset request:** generates a raw token, persists only its SHA-256 hash (`TokenHashUtil`) with a 15-minute expiry — the raw token is never logged or returned; actual email delivery is not implemented yet, so there's currently no way for a user to receive the raw token in this codebase.
- **Password reset confirm:** validates the token (exists, unused, unexpired) via the hash, updates the credential's password hash, marks the token used so it can't be replayed.

**Why the reset token is hashed before storage (`TokenHashUtil.sha256Hex`), the same principle as password hashing:** if the `password_reset_tokens` table were ever read by someone who shouldn't (a backup leak, a compromised DB credential), a stored raw token would let them complete a password reset for that user immediately. Storing only the SHA-256 hash means confirming a reset still requires knowing the original raw token value, which only ever existed in memory when it was generated and (in a complete implementation) emailed to the user. Consuming it once (`markUsed`) then persisting that flag prevents a captured token being replayed after the legitimate user has already used it.

**Registration flow, from this service's side (see [user-profile-service.md](user-profile-service.md) for the full picture):** this service never receives a public `POST /auth/register` call — registration is initiated on user-profile-service, which calls this service's *internal* `POST /internal/credentials` via Feign. That's why `registerCredential` generates the UUID **before** any row exists anywhere else: user-profile-service needs that UUID back in the same call so it can save its own profile row using it as the primary key, keeping the two tables linked without either service generating a conflicting identifier.

## Data model

`Credential` (table `credentials` in `authentication_db`): `user_id` (UUID, PK), `email` (unique), `password_hash` (BCrypt, via Spring Security's `BCryptPasswordEncoder`), `role` (`Role.HR` — Guest is intentionally **not** a stored value; it's the absence of a credential/token, not a row), `created_at`.

`PasswordResetToken`: token hash, `user_id`, `expires_at`, used flag.

See [database.md](database.md) for the full schema table and how this database now lives inside the single shared `mysql-db` container.

This service, and only this service, reads/writes `authentication_db` — no other service is granted access to it, even though they now share a physical MySQL server (see [database.md](database.md) for how the per-service users/grants enforce that). If another service needed to know something about a user's credential (e.g. "does this email exist"), it would have to ask this service through an API, never query the table directly — that boundary is what "each service owns its own data" means in practice here.

## Security

- **Signs** JWTs (the only service that does) — HMAC-SHA256 via `jjwt`, using the shared `JWT_SECRET`.
- Own `SecurityFilterChain`: `/auth/login`, `/auth/reset-password/**`, `/internal/**`, `/actuator/**` are public; everything else requires a valid JWT (defense-in-depth — the Gateway also enforces this).
- Passwords are always `BCrypt`-hashed; the raw password is never persisted, logged, or returned in any response.

**Why this service's own `SecurityFilterChain` re-checks auth when the Gateway already does it:** this is deliberate defense-in-depth, not redundant plumbing. The Gateway is the primary boundary and the one the frontend is expected to go through, but nothing at the network level stops a request from reaching this service's port directly (bypassing the Gateway) inside the Docker network, or in a future deployment where the Gateway's port might be misconfigured. If this service trusted every incoming request unconditionally, that would be a single point of failure — one Gateway misconfiguration and every service becomes wide open. Each service independently verifying the same JWT (with the same shared secret) means the Gateway being correct is a *convenience/single-entry-point*, not the *only* thing standing between an attacker and the data.

## Communication with other services

- **Inbound only** from `user-profile-service` (Feign → `POST /internal/credentials`), resolved via Eureka.
- Never calls another service itself.

This makes authentication-service a "leaf" in the call graph — everything flows into it (from user-profile-service, and indirectly from every service that verifies its tokens), nothing flows out of it. That's consistent with its role: it's the source of truth for identity, so it has no reason to depend on anyone else's data.

## Docker

Standard backend Dockerfile (see [docker.md](docker.md)). `docker-compose.yml` overrides its datasource URL to point at `mysql-db:3306/authentication_db` and waits for `mysql-db`'s healthcheck before starting.

Waiting on the healthcheck (not just "container started") specifically prevents this service from attempting its first datasource connection before MySQL can actually accept one — without it, a cold `docker compose up` could have this service crash-loop on startup purely due to timing, not a real configuration problem.

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

The split between `AuthServiceTest` (mocked repositories — fast, tests pure business logic like "does a wrong password throw `InvalidCredentialsException`") and `AuthServiceIntegrationTest` (a real, if in-memory, database) exists because some bugs only show up with real persistence behavior — e.g. the unique constraint on `email` actually being enforced, or a saved entity's `created_at` actually being populated by Hibernate. Using H2 in `MODE=MySQL` instead of a live MySQL container means these tests run in every environment (a laptop with no Docker running, CI) without sacrificing that real-persistence coverage. `SecurityChainIntegrationTest` proves the same kind of thing `api-gateway`'s equivalent test proves, but for this service's own (defense-in-depth) filter chain: that `/auth/login`, `/auth/reset-password/**`, `/internal/**`, and `/actuator/**` are genuinely public and everything else genuinely isn't.

## How to explain this service in a presentation

"Authentication Service owns identity — credentials, password hashing, JWT issuance, and password reset. It's the only service that signs a JWT; every other service that checks a token only verifies it, using the same shared secret. It deliberately doesn't store any profile information like name or phone — that's User Profile Service's job — the two are linked only by a UUID generated here at registration time. There's no public registration endpoint on this service; registration actually starts on User Profile Service, which calls this service internally, through Feign, to create the credential first, then saves its own profile row using the UUID this service hands back. Passwords are always BCrypt-hashed and never stored or logged in plain text, and a login failure never reveals whether the problem was the email or the password, so the API can't be used to find out which emails are registered."
