# Attrition Analyzer

A microservices-based HR analytics application for exploring employee attrition. HR users log in to search employee records, view six dimensions of attrition analysis, and flag at-risk employees with a comment that other HR users see as a notification. A **Guest** (anyone not logged in) can view a limited attrition summary on the landing page and register to become an HR user — everything else (employee list, employee details, notifications, profile) requires login.

Employee data is not stored by this application — it's retrieved live from an external **Survey API** that acts as the system of record.

> **Source of truth:** this README was written by inspecting the actual repository (code, `docker-compose.yml`, `pom.xml`/`package.json` files, Spring Security configs, and tests), not by copying the original planning documents. Where `docs/technical-plan.md` differs from what was actually built, this README and the linked docs describe what was built.

---

## 1. Project Overview

Attrition Analyzer helps HR teams answer "who is likely to leave, and why?" without spreadsheets. It's six independent Spring Boot microservices behind a single API Gateway, backed by Eureka service discovery, one shared MySQL server (three separate databases), and Kafka for one specific asynchronous flow (flagging an employee → creating a notification). The frontend is a React + Vite single-page app that talks **only** to the Gateway, and runs in its own Docker container.

## 2. What the System Does

- **Guest** (not logged in): views a limited attrition summary on the landing page, can register.
- **HR User** (logged in): full employee directory + search, employee details, all six attrition analyses, flagging employees, notifications, and profile management.

| Area | What it covers |
|---|---|
| Account & Authentication | Register, login, logout, password reset, session expiry |
| Profile Management | View/update your own HR profile |
| Employee Data | Employee list, search, employee details (proxied from the Survey API) |
| Attrition Analysis | Six dimensions: department, job role, compensation, demographics, work-life balance, career progression |
| Notification Management | Flag an employee → notification created asynchronously via Kafka; view/delete your own notifications |
| Guest Experience | Public landing page with a limited attrition preview; register to become an HR user |

## 3. Architecture Overview

```
Frontend (React, own container)
        │  REST, JWT bearer, browser → host-mapped port
        ▼
API Gateway :8080   (JWT validation, CORS, routing)
        │
        ├──► Authentication Service :8081 ──► mysql-db / authentication_db
        ├──► User Profile Service   :8082 ──► mysql-db / user_profile_db
        │        │  Feign, internal only (never through the Gateway)
        │        ▼
        │    Authentication Service  (POST /internal/credentials)
        ├──► Employee Service       :8083 ──► external Survey API
        │        │  publishes
        │        ▼
        │    Kafka: employee.flagged
        │        │  consumes
        │        ▼
        └──► Notification Service  :8084 ──► mysql-db / notification_db
```

Every service registers with Eureka Discovery Service (`:8761`) and is resolved by name — no hard-coded service-to-service addresses. See [docs/backend/api-gateway.md](docs/backend/api-gateway.md) for the exact routing table and auth rules, and [docs/backend/kafka.md](docs/backend/kafka.md) for the flag → notification sequence.

## 4. Service Overview

| Service | Owns | Port | Docs |
|---|---|---|---|
| discovery-service | Eureka registry only | 8761 | [docs/backend/discovery-service.md](docs/backend/discovery-service.md) |
| api-gateway | Routing, CORS, JWT validation | 8080 | [docs/backend/api-gateway.md](docs/backend/api-gateway.md) |
| authentication-service | `authentication_db` (credentials, JWT issuance) | 8081 | [docs/backend/authentication-service.md](docs/backend/authentication-service.md) |
| user-profile-service | `user_profile_db` (name, email, phone) | 8082 | [docs/backend/user-profile-service.md](docs/backend/user-profile-service.md) |
| employee-service | No database — proxies the external Survey API | 8083 | [docs/backend/employee-service.md](docs/backend/employee-service.md) |
| notification-service | `notification_db` (notifications) | 8084 | [docs/backend/notification-service.md](docs/backend/notification-service.md) |
| frontend | React SPA, own container | 3000 (Docker) / 5173 (dev) | [docs/frontend/frontend.md](docs/frontend/frontend.md) |

## 5. Database Overview

One MySQL 8.0 server (`mysql-db`), three separate databases, one dedicated user per database — no shared schema, no cross-service table access.

| Database | Owner | Container |
|---|---|---|
| `authentication_db` | authentication-service | `mysql-db` |
| `user_profile_db` | user-profile-service | `mysql-db` |
| `notification_db` | notification-service | `mysql-db` |

Full schema, ownership rules, and how `docker/mysql/init-databases.sh` creates them: [docs/backend/database.md](docs/backend/database.md).

## 6. Kafka Overview

A single-broker Kafka cluster (KRaft mode, `apache/kafka:3.9.0`) carries exactly one topic, `employee.flagged`, connecting employee-service (producer) to notification-service (consumer), with `eventId` as the idempotency key. Full flow, event schema, and why Kafka is used only here: [docs/backend/kafka.md](docs/backend/kafka.md).

## 7. Docker Overview

`docker-compose.yml` defines the whole stack — 6 backend services, the frontend, one MySQL container, and Kafka — on one bridge network. The frontend is a separate two-stage build (Vite build → nginx) and talks to the Gateway at `http://localhost:8080` from the **browser**, never a Docker-internal hostname. Full container table, dependency graph, health checks, and common commands: [docs/backend/docker.md](docs/backend/docker.md).

## 8. Ports

| Service / Container | Host port | Protocol |
|---|---|---|
| Frontend (Docker) | `3000` | HTTP |
| Frontend (Vite dev server) | `5173` (default, not fixed) | HTTP |
| API Gateway | `8080` | HTTP |
| Authentication Service | `8081` | HTTP |
| User Profile Service | `8082` | HTTP |
| Employee Service | `8083` | HTTP |
| Notification Service | `8084` | HTTP |
| Eureka Discovery Service | `8761` | HTTP |
| Kafka broker | `9092` | Kafka protocol |
| `mysql-db` (all 3 databases) | `3306` | MySQL |
| External Survey API (not part of this repo) | `3232` (default expected) | HTTP |

## 9. Prerequisites

| Tool | Version | Needed for |
|---|---|---|
| JDK | 21 | Building/running any backend service |
| Maven | bundled `mvnw`/`mvnw.cmd` — no separate install needed | Backend builds |
| Node.js | LTS (18+) | Frontend |
| Docker + Docker Compose | recent | Running the full stack |
| An external Survey API | reachable at the URL you configure | Employee Service has no employee data of its own |

**About the Survey API:** by default Employee Service points at `http://localhost:3232` (`http://host.docker.internal:3232` inside Docker). You must have a Survey API instance (from the case study this project is based on) running and reachable there, or override `survey-api.base-url` / the Compose `-Dsurvey-api.base-url` value. Nothing in this repository starts a Survey API for you — it is not one of the containers in `docker-compose.yml`.

## 10. Quick Start

```bash
git clone <this-repo-url>
cd attrition-analyzer
cp .env.example .env
# edit .env and fill in every value

for svc in discovery-service api-gateway authentication-service user-profile-service employee-service notification-service; do
  (cd "$svc" && ./mvnw -q -DskipTests package)   # Windows: .\mvnw.cmd
done

docker compose up -d --build
docker compose ps
```

Point Employee Service at your Survey API if it isn't at the default address (see Prerequisites) — edit `-Dsurvey-api.base-url` in `docker-compose.yml` and `docker compose up -d --build employee-service`.

Full variable-by-variable `.env` reference and running the frontend outside Docker: [docs/frontend/frontend.md](docs/frontend/frontend.md) (frontend) and [docs/backend/docker.md](docs/backend/docker.md) (backend/Compose).

## 11. How to Access the Application

- **Docker (full stack):** open `http://localhost:3000`.
- **Frontend dev server (backend still via Docker):** `cd frontend && npm install && npm run dev`, then open the printed URL (Vite default `http://localhost:5173`) — it talks to the Gateway at `http://localhost:8080` with no extra configuration.
- Visit `/` — the Guest-visible attrition preview loads without logging in.
- Register a new HR account, or log in if you already have one.
- Once logged in: Dashboard, Employees (list → details), flag an employee, see the notification appear, delete it, view/update your profile.
- A Guest clicking Analytics/Employees/Profile/Notifications in the nav is redirected to `/login`.

## 12. Testing Overview

Every service is an independent Maven project — run its tests from its own directory:

```bash
cd <service-directory>
./mvnw test          # Windows: .\mvnw.cmd test
```

Authentication and User Profile's integration tests run against an in-memory H2 database — they don't need Docker running. Each service's docs page lists its exact test classes and what they cover. One test, `employee-service`'s `SurveyApiClientIT`, is **not** run automatically (no Failsafe plugin configured to pick up the `*IT` naming convention) and requires a live Survey API — see [docs/backend/employee-service.md](docs/backend/employee-service.md).

Frontend:

```bash
cd frontend
npm run build   # tsc -b && vite build
npm run lint    # oxlint
```

## 13. Documentation

This README stays high-level. Everything else lives in `docs/`:

- [Authentication Service](docs/backend/authentication-service.md)
- [User Profile Service](docs/backend/user-profile-service.md)
- [Employee Service](docs/backend/employee-service.md) — includes the six attrition-analysis dimensions and why it has no database
- [Notification Service](docs/backend/notification-service.md)
- [API Gateway](docs/backend/api-gateway.md) — routing table and the full Guest-vs-HR auth rules
- [Discovery Service](docs/backend/discovery-service.md)
- [Kafka](docs/backend/kafka.md) — the `employee.flagged` flow end to end
- [Database](docs/backend/database.md) — the single MySQL server, three databases, schema
- [Docker](docs/backend/docker.md) — container reference table, dependency graph, commands
- [Frontend](docs/frontend/frontend.md) — routes, auth state, API connection, Docker

---

## Repository Structure

```
attrition-analyzer/
├── discovery-service/           # Eureka service registry
├── api-gateway/                 # Single entry point, JWT validation, CORS, routing
├── authentication-service/      # Credentials, login, JWT issuance
├── user-profile-service/        # HR profile data, registration
├── employee-service/            # Employee records, search, 6 attrition analyses, flagging (no DB)
├── notification-service/        # Notifications, Kafka consumer
├── frontend/                    # React + Vite + TypeScript SPA (own Dockerfile)
├── docker/mysql/init-databases.sh   # Creates all 3 databases/users in the single mysql-db container
├── docs/                        # Detailed per-service/topic documentation (see above)
├── docker-compose.yml
├── .env.example
├── CLAUDE.md                    # Project rules/conventions for AI-assisted development
└── README.md
```

Each service directory is an **independent** Spring Boot Maven project (own `pom.xml`, `mvnw`/`mvnw.cmd`, `src/`) — this is not a multi-module build.

> **Note on `docs/technical-plan.md`:** it's the original pre-implementation plan; its proposed endpoint names differ from what was actually built (e.g. it proposes `POST /auth/register`; the real implementation uses `POST /users/register`). Treat this README, the linked `docs/` files, and the code as authoritative for endpoints.
