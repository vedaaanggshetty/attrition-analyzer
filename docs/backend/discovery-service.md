# Discovery Service

## Purpose

A single Eureka server. Every other backend service registers itself here on startup and looks up the others by name instead of hard-coded host/port — this is what lets `api-gateway`'s routes say `lb://authentication-service` instead of a fixed address, and what lets `user-profile-service`'s Feign client resolve `authentication-service` without configuration.

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

## How other services use it

Every other service's `application.properties` sets:

```properties
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

overridden in `docker-compose.yml` to `http://discovery-service:8761/eureka/` (the Docker-internal hostname) for every service's `JAVA_OPTS`.

- The Gateway resolves `lb://<service-name>` routes through it.
- `user-profile-service`'s `AuthenticationClient` (a `@FeignClient(name = "authentication-service")`) resolves the target host/port through it — no URL is hard-coded.

## Docker

```yaml
discovery-service:
  build: { context: ./discovery-service }
  ports: [ "8761:8761" ]
```

No dependencies, no database, no healthcheck defined on this container itself (other services depend on it with `condition: service_started`, not `service_healthy`).

## Testing

`DiscoveryServiceApplicationTests` — a context-load smoke test (`contextLoads()`), verifying the Spring context (including `@EnableEurekaServer`) starts without errors. No business logic to unit test.
