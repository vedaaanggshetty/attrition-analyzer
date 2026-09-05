# API Gateway

## Purpose

The single entry point the frontend talks to. Every request from the browser goes to the Gateway on `:8080`; the Gateway routes it to the right backend service by path, and enforces JWT authentication/authorization before the request ever reaches a business service.

## Routing

Routes are configured declaratively in `application.properties` (Spring Cloud Gateway MVC style, resolved via Eureka — `lb://` means "load-balance across whatever Eureka has registered under this name"):

| Path prefix | Routed to |
|---|---|
| `/auth/**` | `lb://authentication-service` |
| `/users/**` | `lb://user-profile-service` |
| `/employees/**` | `lb://employee-service` |
| `/notifications/**` | `lb://notification-service` |

There is no route for `/internal/**` — authentication-service's internal credential-registration endpoint is deliberately **not reachable through the Gateway**; it's called service-to-service via Feign (see [user-profile-service.md](user-profile-service.md)).

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

## How it talks to other services

Only via Eureka-resolved routing (`lb://<service-name>`) — no Feign clients, no direct HTTP calls of its own. It has no database.

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
