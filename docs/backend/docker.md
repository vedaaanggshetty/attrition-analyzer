# Docker

`docker-compose.yml` at the repo root defines the full stack on one bridge network, `attrition-net`. It does **not** run a Survey API container — Employee Service depends on an external Survey API instance that this repo doesn't own or start (see [employee-service.md](employee-service.md) and the root `README.md`'s Prerequisites section); by default it's reached via `http://host.docker.internal:3232` from inside the backend containers.

**Why a single bridge network for everything:** every container attached to `attrition-net` can resolve every other container by its Compose service name (`mysql-db`, `kafka`, `discovery-service`, etc.) via Docker's built-in embedded DNS — this is what makes `jdbc:mysql://mysql-db:3306/...` or `http://discovery-service:8761/eureka/` work at all inside `JAVA_OPTS`. None of this network resolution is visible to, or reachable from, the browser — that boundary (container-to-container vs. browser-to-host) is the single most important networking concept in this whole setup, and the exact reason the frontend can't use those same hostnames (see below).

## Container reference

| Container | Purpose | Image | Host port → container | Depends on | Persistent data |
|---|---|---|---|---|---|
| `discovery-service` | Eureka service registry | built from `./discovery-service` | `8761 → 8761` | — | none |
| `api-gateway` | Single entry point: routing, CORS, JWT validation | built from `./api-gateway` | `8080 → 8080` | `discovery-service` | none |
| `employee-service` | Employee data (proxied from Survey API), attrition analysis, flagging | built from `./employee-service` | `8083 → 8083` | `discovery-service`, `kafka` (healthy) | none (no DB) |
| `kafka` | Message broker (`employee.flagged` topic) | `apache/kafka:3.9.0` | `9092 → 9092` | — | none (no volume configured — topic data is lost on `docker compose down`) |
| `mysql-db` | Single MySQL server hosting all three service databases | `mysql:8.0` | `3306 → 3306` | — | volume `mysql-db-data` |
| `authentication-service` | Login, JWT issuance, credentials | built from `./authentication-service` | `8081 → 8081` | `discovery-service`, `mysql-db` (healthy) | none (data lives in `mysql-db`) |
| `user-profile-service` | Registration, profile view/update | built from `./user-profile-service` | `8082 → 8082` | `discovery-service`, `mysql-db` (healthy) | none (data lives in `mysql-db`) |
| `notification-service` | Notifications, Kafka consumer | built from `./notification-service` | `8084 → 8084` | `discovery-service`, `mysql-db` (healthy), `kafka` (healthy) | none (data lives in `mysql-db`) |
| `frontend` | Static React build served by nginx | built from `./frontend` | `3000 → 80` | `api-gateway` (start order only, not health-gated) | none |

## One MySQL container, three databases

`mysql-db` is a single `mysql:8.0` container hosting three separate databases (`authentication_db`, `user_profile_db`, `notification_db`), each with its own dedicated user and grants. See [database.md](database.md) for the full explanation, the init script, and per-database ownership. This replaced an earlier design with three separate MySQL containers (`authentication-db`, `user-profile-db`, `notification-db`) — those container names no longer appear in `docker-compose.yml`.

The migration itself only touched `docker-compose.yml` (three MySQL service blocks became one, and the three app services' `-Dspring.datasource.url` values were repointed at `mysql-db`) and the previously-unused `docker/mysql/init-databases.sh` — no Java code changed, because every service already read its datasource connection details from environment variables rather than hard-coding them (see [database.md](database.md)).

## Backend Dockerfiles

Every backend service Dockerfile has the same shape — they package a **pre-built jar**, they do not run Maven themselves:

```dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --uid 1001 appuser
COPY target/*.jar app.jar
USER appuser
EXPOSE <service port>
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

This means **you must build the jar with `mvnw package` before `docker compose build`** for any backend service you've changed — the Dockerfile has nothing to compile from otherwise. `JAVA_OPTS` (set per-service in `docker-compose.yml`) carries all the `-D` system property overrides — Eureka URL, datasource URL/credentials, `jwt.secret`, Kafka bootstrap servers, Survey API base URL.

**Why the Dockerfile doesn't run Maven itself (a two-stage Maven-build-then-run image, as many Spring Boot Dockerfiles do):** doing the Maven build as a separate manual step keeps the image trivially simple and fast to build/rebuild once the jar exists (just copying a file), and keeps the Docker build from needing network access to a Maven repository at image-build time. The cost is the extra manual step this doc calls out — forgetting to rebuild the jar after a code change is the single most common way to see "my change isn't showing up in the container," since Docker will happily rebuild an image around a stale jar with no error.

## Frontend Dockerfile

The frontend is a separate, two-stage build (`frontend/Dockerfile`):

1. **Build stage** (`node:20-alpine`): `npm ci`, then `npm run build` (`tsc -b && vite build`) to produce a static `dist/` bundle. `VITE_API_BASE_URL` is passed as a build `ARG`/`ENV` here — Vite bakes env vars into the JS bundle at build time, so this cannot be changed at container-start time the way the backend's `JAVA_OPTS` can.
2. **Runtime stage** (`nginx:1.27-alpine`): copies `dist/` into nginx's web root and serves it on port 80. `frontend/nginx.conf` adds a `try_files $uri $uri/ /index.html` fallback so React Router's client-side routes (e.g. `/employees/42`) work on a direct load/refresh instead of 404ing.

**Why two stages instead of one:** the build stage needs the full Node.js toolchain (npm, the entire `node_modules` tree, TypeScript) just to *produce* the static files — none of that is needed to *serve* them afterward. A multi-stage build lets the final image contain only nginx plus the built `dist/` output, discarding the Node build environment entirely; the shipped container is a small, static-file web server, not a Node.js runtime, which is both smaller and has a much smaller attack surface (no Node runtime or npm packages present in the image that actually runs in production).

### Why the frontend talks to `localhost:8080`, not `api-gateway:8080`

The frontend container's nginx only ever serves static files — it's the **browser**, not the container, that makes the actual API calls (`fetch()` from JavaScript running on the user's machine). The browser cannot resolve `api-gateway` (a Docker-internal hostname on `attrition-net`); it can only reach whatever is published to the host. `VITE_API_BASE_URL` therefore defaults to `http://localhost:8080` (the Gateway's host-mapped port from the *browser's* point of view, not a container-to-container address) — see `frontend/src/lib/apiClient.ts`. Override it only if the Gateway is reachable at some other host-facing address (e.g. a real domain in a non-local deployment).

This is worth stating plainly, since it's the single most common Docker-networking mistake in a multi-container setup: **"can container A resolve container B's name" and "can the browser resolve container B's name" are two completely different questions**, and this project's frontend build only cares about the second one. Getting this backwards (baking a Docker-internal hostname into the frontend build) would produce a frontend that works fine when tested with `curl` from inside the network, but fails entirely for an actual user's browser — a subtle bug worth being able to explain in a viva if asked "why does the frontend need a different address than the backend services use to reach each other?"

## Startup dependencies and health checks

- `mysql-db` and `kafka` each have a `healthcheck` (`mysqladmin ping` / `kafka-broker-api-versions.sh`). Every service that needs them declares `depends_on: <service>: condition: service_healthy`, so they wait for a real health check to pass, not just "container started" — this avoids cold-start races (a backend service trying to connect before MySQL/Kafka can accept connections).
- `discovery-service` uses `condition: service_started` everywhere it's a dependency — Eureka doesn't need to be fully "ready" for other services to start attempting registration; they retry.
- `frontend` depends on `api-gateway` for **start order only** (no `condition`) — it doesn't need the Gateway to be healthy to start serving static files; a page load before the Gateway is ready just means API calls fail until it comes up.

This graduated approach — `service_healthy` where a real dependency exists and failing fast matters, `service_started` where only rough ordering matters, and no dependency at all where none exists — is deliberate rather than applying the strictest check everywhere. Over-using `service_healthy` (e.g. making the frontend wait for the Gateway to be healthy) would slow down every `docker compose up` unnecessarily for a dependency that isn't actually required at container-start time.

## Running it

```bash
# from the repo root
cp .env.example .env        # fill in real values - see README's Environment Variables section
for svc in discovery-service api-gateway authentication-service user-profile-service employee-service notification-service; do
  (cd "$svc" && ./mvnw -q -DskipTests package)   # Windows: .\mvnw.cmd
done

docker compose up -d --build
docker compose ps
docker compose logs -f api-gateway     # or any other service name
```

First cold start can take 1–2 minutes for MySQL to become healthy before the three MySQL-backed services start.

### Rebuilding a single service

```bash
docker compose up -d --build <service-name>
```

For a backend service, re-run `./mvnw package` first if the code changed — the Dockerfile only copies the existing jar.

### Stopping

```bash
docker compose down          # stop + remove containers, keep the mysql-db-data volume
docker compose down -v       # also delete mysql-db-data (destructive - wipes all three databases)
```

### Viewing logs

```bash
docker compose logs -f                    # everything
docker compose logs -f mysql-db           # one container
```

### Resetting just the database

```bash
docker compose down mysql-db
docker volume rm attrition-analyzer_mysql-db-data
docker compose up -d mysql-db
```

This re-runs `docker/mysql/init-databases.sh` against the now-empty volume, recreating all three databases and users from scratch (all existing data is lost).

## How to explain this in a presentation

"The whole stack — six Spring Boot services, the frontend, one MySQL container, and Kafka — runs from a single docker-compose.yml on one Docker network, where every container can resolve every other one by its service name. The backend services build from pre-built jars, so you have to run the Maven build before rebuilding the Docker image. The frontend is a separate two-stage build — Node builds the static files, then a lightweight nginx image actually serves them — and it's the one container with a fundamentally different networking need: it has to talk to the Gateway at a browser-reachable address, localhost:8080, not a Docker-internal hostname, because it's the user's browser making the API calls, not the container itself. Startup ordering is handled with health checks where a real dependency exists — services wait for MySQL and Kafka to actually be ready, not just started, to avoid connecting before they can accept connections."
