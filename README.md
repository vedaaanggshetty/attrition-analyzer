# Attrition Analyzer

## Overview

Attrition Analyzer is a microservices-based application that helps HR users explore employee information and analyze workforce attrition. Attrition is examined across six factors:

- Department
- Job Role
- Compensation
- Demographics
- Work-Life Balance
- Career Progression

Employee information is not stored by the application itself — it is retrieved from an external **Survey API**, which acts as the system of record for employee data.

## User Types

The system supports exactly two user types:

- **Guest** — can view a limited, summarized view of attrition information without an account.
- **HR User** — full access to employee records, search, employee details, attrition analysis, and notifications.

A Guest becomes an HR User by registering. There are no additional roles.

## Key Features

Per the finalized product backlog (current implementation progress is tracked in [Development Status](#development-status)):

**Account & Access**
- HR Registration
- Login / Logout
- Password Reset
- Session Timeout
- Profile Management

**Employee Data**
- Employee Records
- Employee Search
- Employee Details

**Attrition Analysis** — by Department, Job Role, Compensation, Demographics, Work-Life Balance, and Career Progression

**Notifications**
- Notifications
- Notification Comments

**Guest Access**
- Guest Attrition Information

## System Architecture

```
React Frontend → API Gateway → Backend Microservices
```

- **Eureka Discovery Service** — every microservice registers with it so the API Gateway (and other services) can discover them by name. It is a registry, not a stop on the request path.
- **API Gateway** — the single entry point for the frontend; routes requests to the correct microservice.
- **Employee Service** — retrieves employee data from the external **Survey API** rather than owning a database.
- **Notification Service** — publishes/consumes events via **Apache Kafka** and owns the Notification database.
- **Authentication Service** and **User Profile Service** — each own a private database.

For the full set of architecture, flow, and sequence diagrams, see **[Project Flows](docs/project-flow.md)**.

## Microservices

| Service | Port | Responsibility |
|---|---|---|
| Discovery Service | 8761 | Eureka service registry — lets every service register and discover each other by name. |
| API Gateway | 8080 | Single entry point for the frontend; routes requests to backend services. |
| Authentication Service | 8081 | Credentials, login, logout, password reset, session timeout, JWT issuance/validation. |
| User Profile Service | 8082 | Owns HR user profile data; registration (profile half), profile view/update. |
| Employee Service | 8083 | Retrieves and analyzes employee data from the external Survey API; owns no database. |
| Notification Service | 8084 | Creates, views, and deletes notifications with comments; consumes employee events via Kafka. |

## Technology Stack

| Layer | Technology |
|---|---|
| Language / Runtime | Java 21 |
| Application Framework | Spring Boot 4.0.8 |
| Microservices / Cloud | Spring Cloud 2025.1.3, Spring Cloud Gateway, Eureka |
| Security | Spring Security |
| Persistence | Spring Data JPA, MySQL |
| Messaging | Apache Kafka |
| Build | Maven |
| Frontend | React |
| Containerization | Docker / Docker Compose |

## Project Structure

```
attrition-analyzer/
├── discovery-service/          # Eureka service registry
├── api-gateway/                # Single entry point / request routing
├── authentication-service/     # Credentials, login, JWT
├── user-profile-service/       # HR user profile data
├── employee-service/           # Employee data (via Survey API) and attrition analysis
├── notification-service/       # Notifications and Kafka consumer
├── docs/
│   ├── diagrams/                # Reference architecture/flow diagrams (source of truth)
│   ├── project-flow.md          # Mermaid diagrams of all major flows
│   └── technical-plan.md        # Story-to-service mapping, API list, DB design
├── docker-compose.yml
├── CLAUDE.md
└── README.md
```

Each service directory is an independent Spring Boot Maven project (own `pom.xml`, `mvnw`, `src/`).

## Documentation

- **[Project Flows](docs/project-flow.md)** — visual diagrams of the system architecture, user journey, authentication, search, attrition analysis, notifications, service discovery, and database ownership.
- **[Technical Plan](docs/technical-plan.md)** — detailed story-to-service mapping, full API list, database design, and development phases.

## External Survey API

Employee Service retrieves employee records from an external Survey API rather than owning an employee database. This keeps the Employee Service stateless with respect to employee data and treats the Survey API as the external system of record. Connection details for the Survey API are not yet finalized in the project documentation, so none are listed here.

## Running the Project

The backend currently consists of six independent Spring Boot Maven projects. Each is started from its own directory using its Maven wrapper. Start `discovery-service` first so the other services have a Eureka server to register with:

```bash
# 1. Start the Eureka discovery server
cd discovery-service
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run

# 2. In separate terminals, start the remaining services
cd api-gateway && ./mvnw spring-boot:run
cd authentication-service && ./mvnw spring-boot:run
cd user-profile-service && ./mvnw spring-boot:run
cd employee-service && ./mvnw spring-boot:run
cd notification-service && ./mvnw spring-boot:run
```

Once running, the Eureka dashboard at `http://localhost:8761` shows which services have registered.

**Current limitations:** `docker-compose.yml` is not yet configured, so MySQL and Kafka are not provisioned by this repository yet. As a result, `authentication-service`, `user-profile-service`, and `notification-service` cannot fully start until a database connection is configured. `discovery-service`, `api-gateway`, and `employee-service` do not require a database and start normally. There is no `frontend/` directory yet — the React frontend has not been scaffolded.

## Development Status

Attrition Analyzer is under **active development**. All six backend services exist as Spring Boot projects with Eureka-based service discovery configured and verified. Business logic (authentication, employee data integration, attrition analysis, notifications) and the React frontend are not yet implemented. Features listed in this README describe the finalized project scope, not necessarily what is currently built — refer to [docs/technical-plan.md](docs/technical-plan.md) for implementation-phase status.
