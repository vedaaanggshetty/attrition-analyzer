# Database

## One MySQL server, three databases

Authentication, User Profile, and Notification each own their data, but as of this pass they share a single MySQL 8.0 **server** (one container, one image) instead of three separate MySQL containers. Ownership is still per-service — each database has its own name and its own credentials, and no service is granted access to another's database.

| Database | Owner service | Credentials (env vars) |
|---|---|---|
| `authentication_db` | authentication-service | `AUTH_DB_USERNAME` / `AUTH_DB_PASSWORD` |
| `user_profile_db` | user-profile-service | `PROFILE_DB_USERNAME` / `PROFILE_DB_PASSWORD` |
| `notification_db` | notification-service | `NOTIFICATION_DB_USERNAME` / `NOTIFICATION_DB_PASSWORD` |

This is deliberately **one shared MySQL server, not one shared schema** — the databases are separate, the users are separate, and each user's grants are scoped (`GRANT ALL PRIVILEGES ON <their_db>.*`) to only their own database. A service cannot query another service's tables even though they're on the same server.

**Why consolidate to one server instead of keeping three separate MySQL containers:** the original three-container design had each database fully isolated at the infrastructure level (separate container, separate volume, separate root password), which is the strongest possible isolation but also means running, healthchecking, and resourcing three near-identical MySQL processes for a project whose actual data volume doesn't need that. Consolidating to one server keeps the *logical* isolation that actually matters for "each service owns its own data" (separate databases, separate credentials, no cross-database grants) while cutting operational overhead to a single container, a single healthcheck, and a single volume to back up/reset. The trade-off being accepted: all three databases now share the same underlying MySQL process's resources and uptime — if that one container goes down, all three services' data becomes unavailable at once, which wasn't true before. For this project's scale, that trade is a reasonable one; a production system with much higher availability requirements per service might choose differently.

## How the databases get created

`docker/mysql/init-databases.sh` is mounted into the `mysql-db` container at `/docker-entrypoint-initdb.d/init-databases.sh` (see `docker-compose.yml`). The official `mysql:8.0` image runs every script in that directory **once**, the first time the container starts against an empty data volume. The script:

1. Creates the three databases (`CREATE DATABASE IF NOT EXISTS ...`).
2. Creates the three per-service users, each identified by its own password.
3. Grants each user `ALL PRIVILEGES` on its own database only.

Because this only runs on an empty volume, changing the script and re-running `docker compose up` does **nothing** on an existing volume — you'd need to remove the `mysql-db-data` volume (`docker compose down -v`, destructive) to see script changes take effect on a fresh install.

This is standard, unmodified behavior of the official `mysql:8.0` image, not custom logic this project wrote — the image's entrypoint script checks whether `/var/lib/mysql` (the data directory, backed by the `mysql-db-data` volume) is already initialized, and only runs anything in `/docker-entrypoint-initdb.d/` the very first time it finds an empty one. This is exactly why the init script is worth understanding as a one-time bootstrap step, not something that "keeps the databases in sync" on every restart — schema changes after that point are Hibernate's job (`ddl-auto=update`), not this script's.

## Table ownership

Each service manages its own schema via Hibernate (`spring.jpa.hibernate.ddl-auto=update`) — nothing here runs hand-written DDL.

| Table | Database | Owner service | Notable columns |
|---|---|---|---|
| `credentials` | `authentication_db` | authentication-service | `user_id` (UUID, PK, app-generated), `email` (unique), `password_hash` (BCrypt), `role` (`HR` — the only stored role), `created_at` |
| `password_reset_tokens` | `authentication_db` | authentication-service | token hash (never the raw token), `user_id`, `expires_at`, used flag |
| `profiles` | `user_profile_db` | user-profile-service | `user_id` (UUID, PK — same UUID as `credentials`, never generated here), `full_name`, `email` (unique), `phone`, `created_at`, `updated_at` |
| `notifications` | `notification_db` | notification-service | `id` (identity PK), `employee_id`, `employee_name`, `department`, `hr_user_email`, `comment`, `event_id` (unique, nullable — Kafka idempotency key), `created_at` |

Employee Service owns **no database** — see [employee-service.md](employee-service.md) for why.

**Why `ddl-auto=update` (Hibernate auto-generates/updates schema from entities) instead of hand-written migration scripts (Flyway/Liquibase):** for a project at this stage — no production data to migrate carefully, a small number of entities per service, and schema changes usually meaning "add a column" — letting Hibernate derive the schema from `@Entity` classes avoids the overhead of maintaining a separate migration file for every change, and keeps the entity class itself as the single source of truth for what a table looks like. This is a reasonable choice for the current phase; a real production deployment with data that must survive schema changes safely (adding a `NOT NULL` column to a populated table, renaming a column without losing data) would need to move to versioned migrations, since `ddl-auto=update` can't express "here's how to transform existing rows," only "here's what the schema should now look like."

**How UUID linkage across two different databases actually works — worth being explicit about, since it's a recurring theme:** `credentials.user_id` (in `authentication_db`) and `profiles.user_id` (in `user_profile_db`) are the *same value*, but there is no foreign key between them — there can't be; MySQL foreign keys only work within one database/schema, and these are two separate databases (now on the same server, but that's incidental — the same design would work across two entirely different database servers). The two rows are linked only by convention: authentication-service generates the UUID at registration time and hands it to user-profile-service, which stores it verbatim as its own row's primary key. Referential integrity between the two is therefore an application-level guarantee (the registration flow's ordering — see [user-profile-service.md](user-profile-service.md)), not a database-enforced one.

## Connection configuration

Each service's `application.properties` builds its JDBC URL from env vars with local-dev defaults, e.g. (authentication-service):

```properties
spring.datasource.url=jdbc:mysql://${AUTH_DB_HOST:localhost}:${AUTH_DB_PORT:3306}/${AUTH_DB_NAME:authentication_db}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=${AUTH_DB_USERNAME}
spring.datasource.password=${AUTH_DB_PASSWORD}
```

In Docker, `docker-compose.yml` overrides the datasource URL via `JAVA_OPTS -Dspring.datasource.url=jdbc:mysql://mysql-db:3306/<db_name>...` for each of the three services — `mysql-db` is the Docker-internal hostname (only resolvable from other containers on `attrition-net`, which is fine since only backend services connect to it, never the browser).

Locally (no Docker), the defaults point at `localhost:3306`, which works once `docker compose up mysql-db` (or a local MySQL instance) is reachable there.

This env-var-driven, default-having configuration is exactly what made the migration from three MySQL containers to one *not* require touching any Java code — every service already externalized its host/port/db-name/credentials, so the only change needed was what value `docker-compose.yml` injects for those variables (all three now say `mysql-db:3306` instead of three different container names), plus the init script itself. If the connection details had been hard-coded in `application.properties` per service, this consolidation would have required a code change (and a rebuild) in three separate services instead of one Compose file edit.

## How to explain this in a presentation

"We run one MySQL server instead of three, but each service still has its own private database and its own database user — Authentication can't see Notification's tables and vice versa, even though they're on the same physical server. That separation is enforced by MySQL grants, not just convention. The three databases and their users are created once, automatically, by an init script that runs the first time the container starts with an empty volume. Cross-service identity — like Authentication and User Profile both using the same user ID — isn't a foreign key, because they're separate databases; it's just the same UUID value, generated once at registration and passed between the two services. Schema itself is managed by Hibernate directly from our entity classes rather than migration scripts, which is fine for a project at this stage but would need to change before a real production deployment with live data to migrate safely."
