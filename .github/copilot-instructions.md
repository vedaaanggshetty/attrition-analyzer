# Attrition Analyzer — Repository Instructions

## Stack
Java 21, Spring Boot 4, Spring Cloud 2025.1.3, Maven, MySQL, React (frontend, separate project).

## Microservices
- `discovery-service` — Eureka
- `api-gateway` — Spring Cloud Gateway
- `authentication-service` — login, logout, JWT, password hashing/reset
- `user-profile-service` — public registration, profile get/update
- `employee-service` — employee/survey data, attrition analysis
- `notification-service` — notifications, Kafka consumer

## Architecture
React → API Gateway → services. Eureka for service discovery. OpenFeign for synchronous
service-to-service calls (e.g. User Profile → Authentication `/internal/credentials`).
Kafka only for Employee → Notification (async). Employee Service calls the external Survey API.

## Database rules
One MySQL container (`attrition-mysql`), one database per service (e.g. `authentication_db`,
`user_profile_db`). No shared tables, no cross-service DB access, no cross-service foreign keys.

## Ownership
- My scope: `authentication-service`, `user-profile-service`.
- Teammate scope: `discovery-service`, `api-gateway`, `employee-service`, `notification-service`.
- Do not modify teammate services unless integration strictly requires it, and inspect their
  current code first.

## Working rules
- Inspect existing code (and `git status`/`git diff`) before changing anything; reuse working
  implementations instead of rewriting them.
- Make the smallest correct change; do not add speculative features or complexity.
- Never modify `.env`, log secrets/passwords/JWTs, or commit unless explicitly asked.
- Never reset/delete databases, Docker volumes, or branches without explicit need.
- Run relevant tests/build after each change and report concise results.
