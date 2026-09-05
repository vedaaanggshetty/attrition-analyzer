# Database

## One MySQL server, three databases

Authentication, User Profile, and Notification each own their data, but as of this pass they share a single MySQL 8.0 **server** (one container, one image) instead of three separate MySQL containers. Ownership is still per-service — each database has its own name and its own credentials, and no service is granted access to another's database.

| Database | Owner service | Credentials (env vars) |
|---|---|---|
| `authentication_db` | authentication-service | `AUTH_DB_USERNAME` / `AUTH_DB_PASSWORD` |
| `user_profile_db` | user-profile-service | `PROFILE_DB_USERNAME` / `PROFILE_DB_PASSWORD` |
| `notification_db` | notification-service | `NOTIFICATION_DB_USERNAME` / `NOTIFICATION_DB_PASSWORD` |

This is deliberately **one shared MySQL server, not one shared schema** — the databases are separate, the users are separate, and each user's grants are scoped (`GRANT ALL PRIVILEGES ON <their_db>.*`) to only their own database. A service cannot query another service's tables even though they're on the same server.

## How the databases get created

`docker/mysql/init-databases.sh` is mounted into the `mysql-db` container at `/docker-entrypoint-initdb.d/init-databases.sh` (see `docker-compose.yml`). The official `mysql:8.0` image runs every script in that directory **once**, the first time the container starts against an empty data volume. The script:

1. Creates the three databases (`CREATE DATABASE IF NOT EXISTS ...`).
2. Creates the three per-service users, each identified by its own password.
3. Grants each user `ALL PRIVILEGES` on its own database only.

Because this only runs on an empty volume, changing the script and re-running `docker compose up` does **nothing** on an existing volume — you'd need to remove the `mysql-db-data` volume (`docker compose down -v`, destructive) to see script changes take effect on a fresh install.

## Table ownership

Each service manages its own schema via Hibernate (`spring.jpa.hibernate.ddl-auto=update`) — nothing here runs hand-written DDL.

| Table | Database | Owner service | Notable columns |
|---|---|---|---|
| `credentials` | `authentication_db` | authentication-service | `user_id` (UUID, PK, app-generated), `email` (unique), `password_hash` (BCrypt), `role` (`HR` — the only stored role), `created_at` |
| `password_reset_tokens` | `authentication_db` | authentication-service | token hash (never the raw token), `user_id`, `expires_at`, used flag |
| `profiles` | `user_profile_db` | user-profile-service | `user_id` (UUID, PK — same UUID as `credentials`, never generated here), `full_name`, `email` (unique), `phone`, `created_at`, `updated_at` |
| `notifications` | `notification_db` | notification-service | `id` (identity PK), `employee_id`, `employee_name`, `department`, `hr_user_email`, `comment`, `event_id` (unique, nullable — Kafka idempotency key), `created_at` |

Employee Service owns **no database** — see [employee-service.md](employee-service.md) for why.

## Connection configuration

Each service's `application.properties` builds its JDBC URL from env vars with local-dev defaults, e.g. (authentication-service):

```properties
spring.datasource.url=jdbc:mysql://${AUTH_DB_HOST:localhost}:${AUTH_DB_PORT:3306}/${AUTH_DB_NAME:authentication_db}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=${AUTH_DB_USERNAME}
spring.datasource.password=${AUTH_DB_PASSWORD}
```

In Docker, `docker-compose.yml` overrides the datasource URL via `JAVA_OPTS -Dspring.datasource.url=jdbc:mysql://mysql-db:3306/<db_name>...` for each of the three services — `mysql-db` is the Docker-internal hostname (only resolvable from other containers on `attrition-net`, which is fine since only backend services connect to it, never the browser).

Locally (no Docker), the defaults point at `localhost:3306`, which works once `docker compose up mysql-db` (or a local MySQL instance) is reachable there.
