# Attrition Analyzer — Technical Implementation Plan

Derived from the finalized Medium Priority backlog (19 stories, unchanged) and the case study.
No new stories added, no existing stories altered.

---

## Step 1 — Story-to-Technology Mapping

| ID | Story (short) | Microservice | API | Database | Kafka? | Depends On | Frontend? | Auth Required? |
|----|----|----|----|----|----|----|----|----|
| US-001 | Register account | UserProfile Service → (Feign) → Authentication Service | POST /auth/register | UserProfile DB + Authentication DB | No | — | Yes | No |
| US-002 | Login | Authentication Service | POST /auth/login | Authentication DB | No | US-001 | Yes | No |
| US-003 | Logout | Authentication Service | POST /auth/logout | Authentication DB (optional token record) | No | US-002 | Yes | Yes |
| US-004 | Reset password | Authentication Service | POST /auth/reset-password | Authentication DB | No | US-001 | Yes | No (must work when locked out) |
| US-005 | View profile | UserProfile Service | GET /users/profile | UserProfile DB | No | US-002 | Yes | Yes |
| US-006 | Update profile | UserProfile Service | PUT /users/profile | UserProfile DB | No | US-005 | Yes | Yes |
| US-007 | View employee records | Employee Service | GET /employees | None (live from Survey API) | No | US-002 | Yes | Yes |
| US-008 | Search/filter employees | Employee Service | GET /employees?dept=... | None | No | US-007 | Yes | Yes |
| US-009 | View employee details | Employee Service | GET /employees/{id} | None | No | US-007 | Yes | Yes |
| US-010 | Attrition by department | Employee Service | GET /employees/analysis/department | None | No | US-007 | Yes | Yes |
| US-011 | Attrition by job role | Employee Service | GET /employees/analysis/job-role | None | No | US-007 | Yes | Yes |
| US-012 | Attrition by salary | Employee Service | GET /employees/analysis/salary | None | No | US-007 | Yes | Yes |
| US-013 | Attrition by tenure | Employee Service | GET /employees/analysis/tenure | None | No | US-007 | Yes | Yes |
| US-014 | Attrition by overtime | Employee Service | GET /employees/analysis/overtime | None | No | US-007 | Yes | Yes |
| US-015 | Create notification | Notification Service | POST /notifications | Notification DB | **Yes** (consumes cached employee snapshot — see Step 3) | US-009 | Yes | Yes |
| US-016 | Add comments | Notification Service | (field on POST /notifications) | Notification DB | No | US-015 | Yes | Yes |
| US-017 | View notifications | Notification Service | GET /notifications | Notification DB | No | US-015 | Yes | Yes |
| US-018 | Delete notification | Notification Service | DELETE /notifications/{id} | Notification DB | No | US-017 | Yes | Yes |
| US-019 | Guest limited view | Employee Service (via API Gateway public route) | GET /public/employees, GET /public/analysis/department | None | No | US-007, US-010 | Yes | No |

**Notes:**
- US-016 has no separate endpoint — the comment is just a field on the same create-notification call. It's tracked as its own story for backlog visibility, not as separate technical work.
- Employee Service intentionally has **no database** — the case study's diagram shows it connecting only to the external Survey API, not a DB icon. It fetches, filters, and aggregates live.

---

## Step 2 — Microservices

**API Gateway**
Single entry point for the React app. Validates JWTs, applies CORS, and routes requests to the correct downstream service. Owns no business data. Also exposes the unauthenticated `/public/*` routes for Guest access (US-019).

**Eureka Discovery Service**
Lets every other service register itself and find each other by name instead of hardcoded URLs/ports. Implements no stories directly — it's platform plumbing.

**Authentication Service**
Owns login credentials and JWT issuance/validation. Implements US-002, US-003, US-004, and the credential half of US-001. Talks to UserProfile Service (receives the registration handoff via Feign) and to the API Gateway (token validation).

**UserProfile Service**
Owns personal/profile data (name, contact info, role). Implements the profile half of US-001, plus US-005 and US-006. Talks to Authentication Service via Feign during registration to hand off credentials.

**Employee Service**
Fetches, filters, and analyzes employee data from the external Survey API. Implements US-007–US-014 and the data half of US-019. Talks only to the external Survey API — no other internal service calls in, except that Notification Service reads employee snapshots from it (see Step 3).

**Notification Service**
Owns HR notifications (employee reference + comment + timestamp). Implements US-015–US-018. Consumes employee snapshot events from Employee Service via Kafka so it has employee details on hand when a notification is created.

---

## Step 3 — Service Communication

| Interaction | Method | Reason |
|---|---|---|
| React → API Gateway | REST | Standard client-facing entry point |
| API Gateway → all services | REST | Simple routing, no benefit from async here |
| UserProfile Service → Authentication Service (registration) | REST/Feign | One-time, synchronous handoff during signup — needs an immediate success/failure response, matches the case study diagram's "Feign Connect" arrow |
| Employee Service → external Survey API | REST | It's a third-party HTTP API — no other option |
| **Employee Service → Notification Service** | **Kafka** | See below |
| Notification Service → Employee Service (fallback, if no cached snapshot exists) | REST/Feign | Simple synchronous fallback for the rare case a notification is created before any Kafka event arrived |

**Why Kafka, specifically:**
The case study's architecture diagram places Kafka directly between Employee Service and Notification Service, and describes Notification Service as retrieving employee data rather than looking it up live. The simplest, real justification for that: when an HR user views an employee (US-009), Employee Service publishes a small "employee snapshot" event (id, name, department, job role) to a Kafka topic. Notification Service consumes it and keeps a small local cache. Then when the same HR user creates a notification a moment later (US-015), Notification Service already has the employee data on hand and doesn't need to make a live call back to Employee Service (which itself depends on an external Survey API that could be slow or briefly down).

This keeps Kafka's use limited to one clear, justified spot rather than routing everything through it. Everything else stays plain REST, which is simpler to build and test.

---

## Step 4 — Database Design

**Authentication DB (MySQL)**
- Table `credentials`
  - `user_id` (PK, matches UserProfile's user_id)
  - `username` / `email`
  - `password_hash`
  - `role` (HR_USER)
  - `created_at`

**UserProfile DB (MySQL)**
- Table `profiles`
  - `user_id` (PK)
  - `full_name`
  - `email`
  - `phone` (optional)
  - `created_at`, `updated_at`

**Employee Service — no database.** Data is fetched live from the external Survey API on each request.

**Notification DB (MySQL or MongoDB — either works, MongoDB is a fine fit since comments are unstructured text)**
- Collection/Table `notifications`
  - `id` (PK)
  - `employee_id`
  - `employee_name`, `department` (denormalized snapshot, cheap and avoids a join/call on every read)
  - `hr_user_id` (who created it)
  - `comment`
  - `created_at`

- Collection/Table `employee_cache` (small, populated from Kafka)
  - `employee_id` (PK)
  - `employee_name`, `department`, `job_role`
  - `last_updated`

No other tables are needed at this scope. Nothing here needs heavy normalization — this is a small, focused dataset per service.

---

## Step 5 — API List

| Method | Endpoint | Purpose | Auth Required | Service |
|---|---|---|---|---|
| POST | /auth/register | Create a new HR account | No | UserProfile → Authentication |
| POST | /auth/login | Authenticate and issue JWT | No | Authentication |
| POST | /auth/logout | Invalidate current session | Yes | Authentication |
| POST | /auth/reset-password | Request/confirm password reset | No | Authentication |
| GET | /users/profile | View own profile | Yes | UserProfile |
| PUT | /users/profile | Update own profile | Yes | UserProfile |
| GET | /employees | List employee records | Yes | Employee |
| GET | /employees?department=X | Filter employees | Yes | Employee |
| GET | /employees/{id} | View one employee's details | Yes | Employee |
| GET | /employees/analysis/department | Attrition by department | Yes | Employee |
| GET | /employees/analysis/job-role | Attrition by job role | Yes | Employee |
| GET | /employees/analysis/salary | Attrition by salary | Yes | Employee |
| GET | /employees/analysis/tenure | Attrition by years at company | Yes | Employee |
| GET | /employees/analysis/overtime | Attrition by overtime | Yes | Employee |
| POST | /notifications | Create notification (with comment) | Yes | Notification |
| GET | /notifications | View own notifications | Yes | Notification |
| DELETE | /notifications/{id} | Delete own notification | Yes | Notification |
| GET | /public/employees | Guest — limited employee view | No | Employee (via Gateway public route) |
| GET | /public/analysis/department | Guest — limited attrition view | No | Employee (via Gateway public route) |

Not implementing these yet — this is the planning list only.

---

## Step 6 — Development Order

Your proposed order is sound and matches the dependency chain from Step 1. One small refinement: **Guest Access (Phase 7) only touches Employee Service and the Gateway**, so it can technically slot in right after Phase 5 instead of waiting until after Notifications. Keeping it last (as you had it) is also fine and arguably simpler to reason about — either works, so the order below keeps your structure with that noted as optional.

- **Phase 0 — Environment Setup:** JDK, Maven, Spring Boot, Node/React, Docker, MySQL, Kafka, Git.
- **Phase 1 — Microservice Foundation:** Eureka, API Gateway, base service skeletons, shared config.
- **Phase 2 — Authentication:** US-001 (credential half), US-002, US-003, US-004.
- **Phase 3 — User Profile:** US-001 (profile half), US-005, US-006.
- **Phase 4 — Employee Service:** US-007, US-008, US-009, Survey API integration.
- **Phase 5 — Attrition Analysis:** US-010–US-014.
- **Phase 6 — Notification Service:** US-015–US-018, Kafka snapshot cache.
- **Phase 7 — Guest Access:** US-019.
  *(Optional: can move to right after Phase 5, since it has no dependency on Notifications.)*

---

## Step 7 — Antigravity / Windows Environment Setup

1. **JDK:** Install JDK 21 (LTS). Set `JAVA_HOME` to the install path and add `%JAVA_HOME%\bin` to `PATH`.
2. **Maven:** Install Maven, set `MAVEN_HOME`, add `%MAVEN_HOME%\bin` to `PATH`. Verify with `mvn -v`.
3. **Spring Boot:** No separate install — each service is a Maven project using `spring-boot-starter-parent` (3.x). Use Spring Initializr (via Antigravity or start.spring.io) per service.
4. **Node.js / React:** Install Node LTS. Scaffold the frontend with Vite (`npm create vite@latest frontend -- --template react`) — lighter and faster than CRA.
5. **Docker Desktop:** Install with the WSL2 backend enabled. In Docker Desktop settings, point the **Docker data root** to your D: drive (Settings → Resources → Advanced) so container images/volumes don't fill C:.
6. **Kafka & MySQL:** Run both via `docker-compose.yml` at the repo root rather than installing natively — keeps versions consistent and easy to tear down. Map their volumes to a folder on D: (e.g., `D:\attrition-analyzer-data\mysql`, `D:\attrition-analyzer-data\kafka`).
7. **Git:** Install Git for Windows, configure `user.name`/`user.email`, clone the repo onto D: if you'd rather keep C: light (e.g., `D:\projects\attrition-analyzer`).
8. **Environment variables:** Keep secrets (JWT signing key, DB passwords) out of source — use a `.env` file (git-ignored) or `application-local.yml` per service, with a `.env.example` committed for reference.
9. **In Antigravity:** Open the repo root as the workspace, add each service folder as a Maven module/subproject so Antigravity can index them independently, and point the run/debug configuration for the frontend at the Vite dev server port.

---

## Step 8 — Repository Structure

Your proposed structure is good as-is. One addition: a `docs/` folder for this plan and future architecture notes.

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
Microservices application to analyze employee attrition patterns (department, job role,
salary, tenure, overtime) and let HR users flag retention concerns via notifications.
Guest users get a limited, read-only view. Employee data comes from an external Survey API.

## Architecture
React frontend → API Gateway (JWT validation, CORS) → microservices, registered via Eureka.
Kafka carries employee snapshot events from Employee Service to Notification Service.

## Services
- api-gateway — routing, JWT validation, public/guest routes
- discovery-service — Eureka
- authentication-service — login, logout, password reset, JWT issuance (MySQL)
- user-profile-service — registration (profile half), profile view/update (MySQL)
- employee-service — employee retrieval, filtering, attrition analysis (no DB; calls Survey API)
- notification-service — create/view/delete notifications, Kafka consumer (MySQL/MongoDB)

## Technology Stack
Java 21, Spring Boot 3.x, Spring Cloud (Eureka, Gateway, OpenFeign), Kafka, MySQL, MongoDB
(notification service only, if used), React (Vite), Docker Compose, JWT (Spring Security).

## Important Commands
- `docker compose up -d` — start MySQL, Kafka, Survey API container
- `mvn spring-boot:run` — run a service from its module directory
- `npm run dev` — run the frontend
- `mvn test` — run tests for a service

## Coding Rules
- Follow the story list in docs/ — do not add scope not tied to a story.
- Keep each service's database private to that service.
- Map external Survey API fields to internal DTOs — never expose the raw external schema.
- Keep synchronous calls synchronous; only use Kafka where already specified.

## Testing Rules
- Write a test alongside any new endpoint before marking a story complete.
- Run tests after every change and report failures — never claim a test passed without running it.

## Security Rules
- Never commit secrets, passwords, or JWT signing keys — use .env / application-local.yml.
- Passwords are hashed (BCrypt), never stored or logged in plain text.
- Never log JWTs or password fields.

## Rules for Claude Code
- Inspect the repo and this file before making changes.
- Implement one phase/story at a time — do not jump ahead.
- Show a plan before major or structural changes and wait for approval.
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
2. Read CLAUDE.md if it is present, and follow its rules.
3. Propose a plan for this phase and wait for my approval before creating or modifying
   files.

Once I approve the plan, your task is to establish the project foundation:
1. Create the base repository structure if it does not already exist:
   api-gateway, discovery-service, authentication-service, user-profile-service,
   employee-service, notification-service, frontend, docker-compose.yml, README.md.
2. Set up each backend folder as a minimal Spring Boot 3.x / Java 21 Maven project
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
- Services that start successfully and are confirmed working
- Any tests run and their results
- Problems encountered
- What remains before Phase 2 (Authentication) can begin
```