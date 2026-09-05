# API Gateway

## Purpose

The single entry point the frontend talks to. Every request from the browser goes to the Gateway on `:8080`; the Gateway routes it to the right backend service by path, and enforces JWT authentication/authorization before the request ever reaches a business service.

**Why this exists as its own service, not folded into the frontend or one of the business services:** it's the one place that needs to know about *every* backend service (to route to it) and needs to run *before* any business logic (to enforce auth). Putting routing/CORS/JWT-verification logic inside, say, employee-service would mean every other service duplicates it, and the frontend would need to know each service's real address and port — defeating the point of Eureka-based service discovery. Centralizing it here means adding a seventh business service later requires one new route entry, not a change to the frontend or to every existing service.

**What it owns:** routing table (path → service), CORS policy, and the JWT *verification* gate for every request that passes through it. **What it deliberately does not own:** business logic, data, or JWT *issuance* — it only checks a token's signature and claims; it never creates one (only authentication-service does that), and it never talks to a database.

## Routing

Routes are configured declaratively in `application.properties` (Spring Cloud Gateway MVC style, resolved via Eureka — `lb://` means "load-balance across whatever Eureka has registered under this name"):

| Path prefix | Routed to |
|---|---|
| `/auth/**` | `lb://authentication-service` |
| `/users/**` | `lb://user-profile-service` |
| `/employees/**` | `lb://employee-service` |
| `/notifications/**` | `lb://notification-service` |

There is no route for `/internal/**` — authentication-service's internal credential-registration endpoint is deliberately **not reachable through the Gateway**; it's called service-to-service via Feign (see [user-profile-service.md](user-profile-service.md)).

**How a request actually gets routed, step by step:** Spring Cloud Gateway MVC matches the incoming path against each route's `Path=` predicate (checked in the order they're numbered, `routes[0]` through `routes[3]`) and picks the first one that matches. The `uri` (`lb://authentication-service`, etc.) is not a real address — `lb://` tells Spring Cloud LoadBalancer to ask Eureka "give me an instance registered as `authentication-service`" and substitute its real host:port at request time. This is what makes the routing table stay correct even if a service's container is recreated (new IP) or scaled to multiple instances — the Gateway never hard-codes an address. If Eureka has no healthy instance for a route's target service, the request fails with a connection/lookup error rather than a route mismatch.

## Security (`SecurityConfig.java`)

The Gateway is where guest-vs-HR access is actually enforced — not in the frontend, and not (for most routes) in the individual services. Its `SecurityFilterChain`:

```java
.requestMatchers("/auth/login").permitAll()
.requestMatchers("/users/register").permitAll()
.requestMatchers("/auth/reset-password/**").permitAll()
.requestMatchers("/actuator/**").permitAll()
.requestMatchers(HttpMethod.GET, "/employees/analysis/**").permitAll()
.anyRequest().authenticated()
```

So: login, registration, password reset, health checks, and the six `GET /employees/analysis/*` attrition-summary endpoints are reachable without a token (this is US-21 — the only Guest-visible business data). **Everything else** — `/employees`, `/employees/{id}`, `/employees/{id}/flag`, all of `/notifications/**`, `/users/me` — requires `Authorization: Bearer <token>` and returns `401` without one. Missing/invalid/expired tokens on a protected route return `401` via a custom `authenticationEntryPoint`, not Spring Security's default `403`.

`JwtAuthenticationFilter` (`OncePerRequestFilter`) runs before `UsernamePasswordAuthenticationFilter`: it reads the `Authorization` header, validates the token via `JwtService`, and — on success — populates `SecurityContextHolder` with an `AuthenticatedUser` principal (`userId`, `email`, `role`) and a `ROLE_<role>` authority. On any failure it just leaves the request unauthenticated and continues the chain; the actual `401` comes from the `authorizeHttpRequests` rules above, not from the filter itself.

`JwtService` only **verifies** tokens here (`Jwts.parser().verifyWith(...)`) — the Gateway never signs a JWT; only authentication-service does that.

**What actually happens to a request, end to end:**

```
Browser
  │  Authorization: Bearer <token>
  ▼
API Gateway
  │  1. JwtAuthenticationFilter runs first: extracts the header, calls JwtService
  │     to verify the signature (shared JWT_SECRET) and expiry.
  │     - No header, or verification fails → filter does nothing, request
  │       continues UNAUTHENTICATED (no exception thrown here).
  │     - Verification succeeds → SecurityContextHolder gets an
  │       AuthenticatedUser (userId, email, role) + ROLE_<role> authority.
  │  2. authorizeHttpRequests rules run next: is this exact path/method in
  │     the permitAll list? If yes, request proceeds regardless of step 1.
  │     If no, Spring Security requires an authenticated context from step 1
  │     - missing/failed auth here is what actually produces the 401.
  │  3. Request is routed (lb://<service>) to the matched backend service.
  ▼
Backend service (sees only the original Authorization header - the Gateway
forwards it unchanged; it does not translate the JWT into some other format)
```

The split between steps 1 and 2 is deliberate: authentication (do we know who this is) and authorization (are they allowed here) are two different concerns, handled by two different pieces of Spring Security's filter chain, which is why a bad/missing token doesn't error out immediately — it only becomes a problem at step 2, for a route that actually requires it.

**Claims carried in every token**, set by authentication-service and read here: `sub` (the user's UUID — the stable identity), `email`, `role` (currently always `HR`), `iat`, `exp`. The Gateway's `JwtAuthenticationFilter` reads `sub`, `email`, and `role` to build the `AuthenticatedUser` principal; it never needs to call another service to know who's asking.

**On failure:** an expired, tampered, or malformed token makes `JwtService.parseClaims` return `Optional.empty()` (it never throws out of the filter) — the filter simply doesn't populate the security context, and the request falls through to the `authorizeHttpRequests` rules as unauthenticated. For any route requiring auth, that produces a `401` via the custom `authenticationEntryPoint` (`response.sendError(SC_UNAUTHORIZED)`) — not Spring Security's default `403` "Forbidden", which would incorrectly imply the caller was identified but not permitted.

### CORS

```java
configuration.setAllowedOriginPatterns(List.of("http://localhost:*"));
configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
configuration.setAllowCredentials(true);
```

This is the **only** place CORS is configured in the project — every other service is reached only through this Gateway, never directly by the browser.

## Important classes

| Class | Role |
|---|---|
| `SecurityConfig` | The `authorizeHttpRequests` rules above, CORS config, the custom 401 entry point |
| `JwtAuthenticationFilter` | Reads/validates the Bearer token, populates the security context |
| `JwtService` | Verify-only JWT parsing (shared secret, same as every other service that validates tokens) |
| `AuthenticatedUser` | The principal type populated on successful auth (`userId`, `email`, `role`) |

Each class has exactly one job — `SecurityConfig` declares *what* the rules are, `JwtAuthenticationFilter` is the *mechanism* that runs those rules per-request, `JwtService` is the *cryptographic* verification logic (kept separate so it can be unit-tested without a servlet context), and `AuthenticatedUser` is just a data carrier. This separation mirrors the same four-class shape used in every other service that validates JWTs (authentication-service, user-profile-service) — a deliberate consistency choice even though there's no shared library.

## How it talks to other services

Only via Eureka-resolved routing (`lb://<service-name>`) — no Feign clients, no direct HTTP calls of its own. It has no database.

The Gateway is a pure **reverse proxy with a security gate** — it never composes data from multiple services or makes its own business decisions. This keeps it simple and stateless: it can be restarted, redeployed, or (in principle) scaled to multiple instances without any coordination concern, since it holds no session state (`SessionCreationPolicy.STATELESS`) and no data of its own.

## Docker

```yaml
api-gateway:
  build: { context: ./api-gateway }
  ports: [ "8080:8080" ]
  environment:
    JAVA_OPTS: -Deureka.client.service-url.defaultZone=http://discovery-service:8761/eureka/ -Djwt.secret=${JWT_SECRET}
  depends_on:
    - discovery-service
```

`8080` is the one port the frontend (and any external client) is meant to call — see [frontend.md](../frontend/frontend.md) and [docker.md](docker.md) for why the frontend must use `localhost:8080`, never a Docker-internal hostname.

## Testing

| Test | Covers |
|---|---|
| `ApiGatewayApplicationTests` | Context loads |
| `JwtServiceTest` | Token parsing/verification (valid, expired, tampered, wrong secret) |
| `JwtAuthenticationFilterTest` | Filter populates/doesn't populate the security context correctly |
| `SecurityChainIntegrationTest` | End-to-end route-level auth rules (which paths are public vs. require a token) |

Together these tests prove the two things that actually matter for a security boundary: that a *valid* token is accepted and correctly turns into an authenticated principal (`JwtServiceTest`, `JwtAuthenticationFilterTest`), and that the *route rules themselves* are correct — i.e. a change to `SecurityConfig` that accidentally made `/employees` public, or made `/employees/analysis/**` require auth, would fail `SecurityChainIntegrationTest` rather than only being caught by manual testing or, worse, in production.

## How to explain this service in a presentation

"The API Gateway is the single door into the system — the frontend never talks to any other service directly, only to the Gateway on port 8080. It does two jobs: it routes requests to the right backend service by path, using Eureka to find that service's actual address instead of a hard-coded one, and it's the main security checkpoint — it validates the JWT on every request and decides which routes are public, like login and the guest-visible attrition summary, versus which need a valid token, like the employee list or notifications. It doesn't issue tokens itself — only Authentication Service does that — it only verifies them, using the same shared secret every service that checks tokens uses. If the token's missing or invalid on a protected route, the Gateway returns 401 before the request ever reaches a business service, so none of the other services have to duplicate that logic."
