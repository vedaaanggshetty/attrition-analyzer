# Attrition Analyzer — Technical Implementation Plan

Derived from the finalized Product Backlog (`Attrition_Analyzer_Product_Backlog_Corrected.xlsx`) — 21 stories across 6 epics. No new stories added, no existing stories altered.

**Technology baseline (fixed — do not change):** Java 21 · Spring Boot 4.0.8 · Spring Cloud 2025.1.3 · Maven

---

## Step 1 — Story-to-Technology Mapping

| ID | Story (short) | Microservice | API | Database | Kafka? | Depends On | Auth Required? |
|----|----|----|----|----|----|----|----|
| US-01 | Register as HR | UserProfile Service → (Feign) → Authentication Service | POST /auth/register | UserProfile DB + Authentication DB | No | — | No |
| US-02 | HR Login | Authentication Service | POST /auth/login | Authentication DB | No | US-01 | No |
| US-03 | HR Logout | Authentication Service | POST /auth/logout | Authentication DB | No | US-02 | Yes |
| US-04 | Reset HR Password | Authentication Service | POST /auth/reset-password | Authentication DB | No | US-01 | No |
| US-05 | Session Timeout | Authentication Service / API Gateway | (enforced on JWT validation, not a standalone endpoint) | Authentication DB | No | US-02 | Yes |
| US-06 | View HR Profile | UserProfile Service | GET /users/profile | UserProfile DB | No | US-02 | Yes |
| US-07 | Update HR Profile | UserProfile Service | PUT /users/profile | UserProfile DB | No | US-06 | Yes |
| US-08 | View Employee Records | Employee Service | GET /employees | None (live from Survey API) | No | US-02 | Yes |
| US-09 | Search Employee Information | Employee Service | GET /employees?property=value | None | No | US-08 | Yes |
| US-10 | View Employee Details | Employee Service | GET /employees/{id} | None | No | US-08 | Yes |
| US-11 | Attrition by Department | Employee Service | GET /employees/analysis/department | None | No | US-08 | Yes |
| US-12 | Attrition by Job Role | Employee Service | GET /employees/analysis/job-role | None | No | US-08 | Yes |
| US-13 | Attrition by Compensation | Employee Service | GET /employees/analysis/compensation | None | No | US-08 | Yes |
| US-14 | Attrition by Demographics | Employee Service | GET /employees/analysis/demographics | None | No | US-08 | Yes |
| US-15 | Attrition by Work-Life Balance | Employee Service | GET /employees/analysis/work-life-balance | None | No | US-08 | Yes |
| US-16 | Attrition by Career Progression | Employee Service | GET /employees/analysis/career-progression | None | No | US-08 | Yes |
| US-17 | Create Employee Notification | Notification Service | POST /notifications | Notification DB | **Yes** (employee snapshot cache — see Step 3) | US-10 | Yes |
| US-18 | Add Comment to Notification | Notification Service | (field on POST /notifications) | Notification DB | No | US-17 | Yes |
| US-19 | View My Notifications | Notification Service | GET /notifications | Notification DB | No | US-17 | Yes |
| US-20 | Delete My Notification | Notification Service | DELETE /notifications/{id} | Notification DB | No | US-19 | Yes |
| US-21 | Explore Attrition Info as Guest | Employee Service (public Gateway route) | GET /public/analysis/department (+ other analysis endpoints, limited) | None | No | US-11 | No |

**Notes:**
- US-18 has no separate endpoint — the comment is a field on the same create-notification call.
- US-05 (Session Timeout) is not its own API call — it's enforced whenever the Gateway validates a JWT and finds it expired/inactive; the "feature" is the expiry behavior itself.
- US-21 is deliberately narrow per its acceptance criteria: attrition summaries only, **not** `/employees` or `/employees/{id}` — those stay HR-only.
- Employee Service still has **no database** — all six analysis stories (US-11–US-16) compute on live data pulled from the Survey API.

---

## Step 2 — Microservices

**API Gateway** — Single entry point for the React app. Validates JWTs (including expiry from US-05), applies CORS, and routes to the correct service. Exposes the narrow `/public/*` routes for Guest access (US-21). Owns no business data.

**Eureka Discovery Service** — Lets every service register and find each other by name. Implements no story directly.

**Authentication Service** — Owns credentials and JWT issuance/validation/expiry. Implements US-02, US-03, US-04, US-05, and the credential half of US-01. Talks to UserProfile Service (Feign handoff on registration) and the API Gateway.

**UserProfile Service** — Owns profile data. Implements the profile half of US-01, plus US-06 and US-07. Talks to Authentication Service via Feign during registration.

**Employee Service** — Fetches, filters, and analyzes employee data from the external Survey API. Implements US-08–US-16 and the data half of US-21. No inbound calls from other services except Notification Service reading its snapshot events.

**Notification Service** — Owns notifications (employee reference + comment + timestamp). Implements US-17–US-20. Consumes employee snapshot events from Employee Service via Kafka.

---

## Step 3 — Service Communication

| Interaction | Method | Reason |
|---|---|---|
| React → API Gateway | REST | Standard client-facing entry point |
| API Gateway → all services | REST | Simple routing, no async benefit |
| UserProfile Service → Authentication Service (US-01 registration) | REST/Feign | One-time synchronous handoff, needs an immediate success/failure result |
| Employee Service → external Survey API | REST | Third-party HTTP API |
| **Employee Service → Notification Service** | **Kafka** | See below |
| Notification Service → Employee Service (fallback only) | REST/Feign | Rare case: create a notification before any snapshot event has arrived |

**Why Kafka:** When an HR user views an employee (US-10), Employee Service publishes a small employee snapshot event (id, name, department, job role) to Kafka. Notification Service consumes it and keeps a small local cache, so when the same HR user creates a notification a moment later (US-17), the data is already on hand — no live dependency on Employee Service (and transitively, the external Survey API) at the exact moment of writing a notification. Everything else stays plain REST/Feign.

---

## Step 4 — Database Design

**Authentication DB (MySQL)**
- `credentials`: `user_id` (PK), `username`/`email`, `password_hash`, `role`, `created_at`
- (US-05) session/token expiry is handled via JWT `exp` claim — no separate session table required unless you choose to support server-side logout/blacklisting.

**UserProfile DB (MySQL)**
- `profiles`: `user_id` (PK), `full_name`, `email`, `phone`, `created_at`, `updated_at`

**Employee Service — no database.** All employee/attrition data is fetched live from the Survey API.

**Notification DB (MySQL or MongoDB)**
- `notifications`: `id`, `employee_id`, `employee_name`, `department`, `hr_user_id`, `comment`, `created_at`
- `employee_cache` (populated via Kafka): `employee_id` (PK), `employee_name`, `department`, `job_role`, `last_updated`

---

## Step 5 — API List

| Method | Endpoint | Purpose | Auth | Service |
|---|---|---|---|---|
| POST | /auth/register | US-01 register | No | UserProfile → Authentication |
| POST | /auth/login | US-02 login | No | Authentication |
| POST | /auth/logout | US-03 logout | Yes | Authentication |
| POST | /auth/reset-password | US-04 reset password | No | Authentication |
| GET | /users/profile | US-06 view profile | Yes | UserProfile |
| PUT | /users/profile | US-07 update profile | Yes | UserProfile |
| GET | /employees | US-08 view records | Yes | Employee |
| GET | /employees?property=value | US-09 search | Yes | Employee |
| GET | /employees/{id} | US-10 view details | Yes | Employee |
| GET | /employees/analysis/department | US-11 | Yes | Employee |
| GET | /employees/analysis/job-role | US-12 | Yes | Employee |
| GET | /employees/analysis/compensation | US-13 | Yes | Employee |
| GET | /employees/analysis/demographics | US-14 | Yes | Employee |
| GET | /employees/analysis/work-life-balance | US-15 | Yes | Employee |
| GET | /employees/analysis/career-progression | US-16 | Yes | Employee |
| POST | /notifications | US-17 + US-18 (comment is a field) | Yes | Notification |
| GET | /notifications | US-19 | Yes | Notification |
| DELETE | /notifications/{id} | US-20 | Yes | Notification |
| GET | /public/analysis/{dimension} | US-21 (limited set, no /employees or /employees/{id}) | No | Employee (via Gateway public route) |

Not implementing these yet — planning list only.

---

## Step 6 — Development Order

- **Phase 0 — Environment:** JDK 21, Maven, Spring Boot 4.0.8, Spring Cloud 2025.1.3, Node/React, Docker, MySQL, Kafka, Git.
- **Phase 1 — Microservice Foundation:** Eureka, API Gateway, service skeletons, health checks.
- **Phase 2 — Authentication:** US-01 (credential half), US-02, US-03, US-04, US-05.
- **Phase 3 — User Profile:** US-01 (profile half), US-06, US-07.
- **Phase 4 — Employee Service:** US-08, US-09, US-10, Survey API integration.
- **Phase 5 — Attrition Analysis:** US-11–US-16.
- **Phase 6 — Notification Service:** US-17–US-20, Kafka snapshot cache.
- **Phase 7 — Guest Experience:** US-21, public Gateway routes, Guest restrictions.
  *(Could move earlier since it only needs Employee Service + Gateway — keeping it last is simpler to reason about either way.)*

---

## Step 7 — Antigravity / Windows Environment Setup

1. **JDK:** Install JDK 21. Set `JAVA_HOME`, add `%JAVA_HOME%\bin` to `PATH`.
2. **Maven:** Install, set `MAVEN_HOME`, verify with `mvn -v`.
3. **Spring Boot / Spring Cloud:** Each service is a Maven project on `spring-boot-starter-parent` **4.0.8**, with Spring Cloud BOM **2025.1.3** for Eureka/Gateway/OpenFeign. Scaffold via start.spring.io or Antigravity's project templates — confirm the generator offers exactly these versions before accepting defaults.
4. **Node.js / React:** Node LTS, scaffold frontend with Vite.
5. **Docker Desktop:** WSL2 backend; move the Docker data root to D: (Settings → Resources → Advanced).
6. **Kafka & MySQL:** Run via `docker-compose.yml` at repo root; map volumes to D: (e.g., `D:\attrition-analyzer-data\mysql`, `...\kafka`).
7. **Git:** Configure user.name/email; clone to D: if you want to keep C: light.
8. **Environment variables:** Secrets (JWT signing key, DB passwords) via `.env` (git-ignored) with a committed `.env.example`.
9. **In Antigravity:** Open repo root as workspace, add each service as a Maven module, point frontend run config at the Vite dev server.

---

## Step 8 — Repository Structure
  
```
attrition-analyzer/
  api-gateway/
  discovery-service/
  authentication-service/
  user-profile-service/
  employee-service/
  notification-service/
  frontend/
  docker-compose.yml
  .env.example
  docs/
  CLAUDE.md
  README.md
```

---

## Step 9 — CLAUDE.md

```markdown
# Attrition Analyzer — CLAUDE.md

## Project Overview
Microservices application analyzing employee attrition (department, job role, compensation,
demographics, work-life balance, career progression). HR users flag retention concerns via
notifications. Guests get a limited attrition-summary view only — no individual employee
records, no HR-only features. Employee data comes from an external Survey API.

## Architecture
React → API Gateway (JWT validation, CORS, public Guest routes) → microservices via Eureka.
Kafka carries employee snapshot events from Employee Service to Notification Service.

## Services
- api-gateway — routing, JWT validation, public/guest routes
- discovery-service — Eureka
- authentication-service — login, logout, password reset, session timeout, JWT (MySQL)
- user-profile-service — registration (profile half), profile view/update (MySQL)
- employee-service — records, search, details, 6 attrition analyses (no DB; calls Survey API)
- notification-service — create/view/delete notifications, Kafka consumer (MySQL/MongoDB)

## Technology Stack (fixed)
Java 21, Spring Boot 4.0.8, Spring Cloud 2025.1.3, Maven, Kafka, MySQL, React (Vite),
Docker Compose, JWT via Spring Security. Do not change these versions without explicit
instruction.

## Important Commands
- `docker compose up -d` — MySQL, Kafka, Survey API container
- `mvn spring-boot:run` — run a service from its module directory
- `npm run dev` — run the frontend
- `mvn test` — run tests for a service

## Coding Rules
- Follow the 21-story backlog in docs/ — do not add scope not tied to a story.
- Keep each service's database private to that service.
- Map external Survey API fields to internal DTOs — never expose the raw external schema.
- Keep synchronous calls synchronous; only use Kafka for the Employee→Notification snapshot.
- Guest-facing endpoints must stay limited to attrition summaries — never expose
  /employees or /employees/{id} without authentication.

## Testing Rules
- Write a test alongside any new endpoint before marking a story complete.
- Run tests after every change and report failures — never claim a test passed without running it.

## Security Rules
- Never commit secrets, passwords, or JWT signing keys — use .env / application-local.yml.
- Passwords hashed (BCrypt), never stored or logged in plain text.
- Never log JWTs or password fields.

## Rules for Claude Code
- Inspect the repo and this file before making changes.
- Implement one phase/story at a time — do not jump ahead.
- Show a plan before major/structural changes and wait for approval.
- Never make destructive DB/infrastructure changes without explicit confirmation.
- Summarize files changed, tests added/run, and remaining work at the end of each task.
```

---

## Step 10 — First Claude Code Prompt

Paste this into Claude Code from the repository root in Antigravity:

```
You are working on the Attrition Analyzer project. This is Phase 1 (Microservice
Foundation) only — do not implement authentication, employee data, analysis, or
notifications yet.

Before making any changes:
1. Inspect the current repository structure and report exactly what already exists.
2. Read CLAUDE.md if it is present, and follow its rules — including the fixed technology
   baseline (Java 21, Spring Boot 4.0.8, Spring Cloud 2025.1.3).
3. Propose a plan for this phase and wait for my approval before creating or modifying
   files.

Once I approve the plan, your task is to establish the project foundation:
1. Create the base repository structure if it does not already exist:
   api-gateway, discovery-service, authentication-service, user-profile-service,
   employee-service, notification-service, frontend, docker-compose.yml, README.md.
2. Set up each backend folder as a minimal Spring Boot 4.0.8 / Java 21 Maven project
   (no business logic yet — just a valid, runnable skeleton).
3. Set up discovery-service as a working Eureka server.
4. Set up api-gateway as a working Spring Cloud Gateway that registers with Eureka.
5. Add a basic health check endpoint to each service (e.g., Spring Boot Actuator
   /actuator/health).
6. Add a basic README.md explaining how to run the project locally.
7. Run each service to confirm it starts successfully and registers with Eureka where
   applicable. Run any tests that exist.
8. Do NOT implement registration, login, employee data, analysis, or notification
   features in this task — that comes in later phases.

When finished, summarize:
- Files/folders created or changed
- Services confirmed working and registering with Eureka
- Any tests run and their results
- Problems encountered
- What remains before Phase 2 (Authentication) can begin
```
