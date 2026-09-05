# Discovery Service

## Purpose

A single Eureka server. Every other backend service registers itself here on startup and looks up the others by name instead of hard-coded host/port — this is what lets `api-gateway`'s routes say `lb://authentication-service` instead of a fixed address, and what lets `user-profile-service`'s Feign client resolve `authentication-service` without configuration.

**Why this exists as a separate service instead of, say, a static config file listing every service's address:** in Docker (and in any real deployment), a service's actual IP address isn't fixed — a container gets a new address every time it's recreated (this project hit exactly that during testing: recreating `employee-service` left a stale, unreachable address cached elsewhere until the registration naturally expired). Hard-coding addresses would mean every redeploy of any one service potentially breaks every other service that calls it. Eureka solves this by having each service announce "I'm `employee-service`, currently reachable at this address" on startup and periodically thereafter (heartbeat), so callers always ask a lookup service for the *current* address instead of assuming a fixed one.

**What it owns:** the registry itself — the live list of "which service names map to which currently-running instances." **What it does not own:** any business logic, routing decisions, or security — it's pure infrastructure. It doesn't even know what `/employees` or `/auth/login` mean; it only knows service *names* (`employee-service`, `authentication-service`, etc.), not their internal routes.

## Implementation

The entire service is `DiscoveryServiceApplication.java` (`@SpringBootApplication` + `@EnableEurekaServer`) and `application.properties`:

```properties
spring.application.name=discovery-service
server.port=8761
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
```

`register-with-eureka=false` / `fetch-registry=false` mean the registry doesn't register itself as a client of itself or try to fetch a peer registry — this is a single-node Eureka setup, not a cluster.

There are no controllers, services, or DTOs — everything is Eureka's built-in server behavior (registration endpoint, heartbeat/lease renewal, the dashboard UI at `/`).

**What happens on the wire, conceptually:** when, say, `authentication-service` starts, its Eureka *client* (a dependency every business service has, configured only by `spring.application.name` and `eureka.client.service-url.defaultZone`) sends a `POST` to this registry announcing its instance (hostname, port, app name). From then on it sends a heartbeat roughly every 30 seconds to renew its lease; if heartbeats stop (a crash, a container being killed), the registry evicts that instance after the lease expires so callers stop being routed to a dead address. This project observed that eviction delay firsthand: after recreating a service's container mid-session, requests briefly hit the old (now-unreachable) address until the stale lease timed out.

## How other services use it

Every other service's `application.properties` sets:

```properties
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

overridden in `docker-compose.yml` to `http://discovery-service:8761/eureka/` (the Docker-internal hostname) for every service's `JAVA_OPTS`.

- The Gateway resolves `lb://<service-name>` routes through it.
- `user-profile-service`'s `AuthenticationClient` (a `@FeignClient(name = "authentication-service")`) resolves the target host/port through it — no URL is hard-coded.

Every backend service in this project (Gateway included) registers with it — the only two things in the whole stack that *don't* go through Eureka are the external Survey API (an outside system, reached via a configured URL, not a Eureka name — see [employee-service.md](employee-service.md)) and MySQL/Kafka (infrastructure, addressed by their Docker Compose service names directly, since they aren't Spring-based Eureka clients).

## Docker

```yaml
discovery-service:
  build: { context: ./discovery-service }
  ports: [ "8761:8761" ]
```

No dependencies, no database, no healthcheck defined on this container itself (other services depend on it with `condition: service_started`, not `service_healthy`).

This is why `condition: service_started` (not `service_healthy`) is enough for every dependent service — Eureka doesn't need to be fully warmed up before other services can start attempting registration; each service's Eureka client simply retries until the registry answers, which is a normal, expected part of Eureka client behavior (not an error condition to guard against with a strict health-gate).

## Testing

`DiscoveryServiceApplicationTests` — a context-load smoke test (`contextLoads()`), verifying the Spring context (including `@EnableEurekaServer`) starts without errors. No business logic to unit test.

This is intentionally the thinnest test in the whole project — there's no business logic here to unit test, so the only thing worth proving is "does the Eureka server actually come up," which a full Spring context load already covers as a side effect.

## How to explain this service in a presentation

"Discovery Service is a single-node Eureka server — every other service registers itself here on startup, so nobody in the system needs to hard-code another service's address. The Gateway resolves its routes through it, and any service that calls another directly, like User Profile calling Authentication via Feign, resolves the target the same way. It's pure infrastructure — no business logic, no database, no security — it just answers 'where is service X currently running?' This matters most in Docker, where a container's address changes every time it's recreated; without Eureka, redeploying any one service would risk breaking every other service that talks to it."
