# Attrition Analyzer

## Project Overview

Attrition Analyzer is a microservices-based application that helps HR users analyze employee attrition and create notifications based on employee information.

Guest users have limited access to the application.

Employee data is obtained from the external Survey API provided in the case study.

## Project Scope

The finalized Attrition Analyzer backlog consists of 21 approved user stories.

Current functional areas:

* Account & Authentication
* Profile Management
* Employee Data
* Attrition Analysis
* Notification Management
* Guest Experience

Attrition analysis terminology:

* Department
* Job Role
* Compensation
* Demographics
* Work-Life Balance
* Career Progression

Do not revert to earlier terminology (e.g. Salary/Overtime) or an earlier 19-story version of the backlog.

## User Types

There are only two user types:

* Guest
* HR User

Intended flow: Guest → Register → HR User.

Guests have limited access according to the finalized requirements and project documentation. Do not invent additional Guest permissions or restrictions, and do not introduce additional roles beyond Guest and HR User.

## Technology Stack

* Java 21
* Spring Boot 4.0.8
* Spring Cloud 2025.1.3
* Maven
* React
* MySQL
* Kafka
* Eureka Service Discovery
* Spring Cloud Gateway
* Spring Security + JWT
* Docker / Docker Compose
* Git

## Microservices

### discovery-service

Eureka Service Discovery.

### api-gateway

Single entry point for the frontend.
Responsible for routing requests and later JWT/security handling.

### authentication-service

Responsible for:

* Login
* Logout
* Password reset
* JWT authentication
* User credentials

### user-profile-service

Responsible for:

* User registration/profile information
* Viewing profile
* Updating profile

### employee-service

Responsible for:

* Fetching employee data from the Survey API
* Employee search/filtering
* Employee details
* Attrition analysis

### notification-service

Responsible for:

* Creating notifications
* Adding comments
* Viewing notifications
* Deleting notifications

### frontend

React (Vite) application used by HR and Guest users, maintained as a separate project from the backend microservices.

## Architecture

Frontend
→ API Gateway
→ Microservices

Eureka is used for service discovery.

Employee Service communicates with the external Survey API.

Kafka will be used where asynchronous communication provides a clear benefit, particularly for the notification flow.

Do not use Kafka for simple synchronous request/response operations.

## Project Structure

Each backend microservice is an independent Spring Boot Maven project.

Do not create a single multi-module Spring Boot application unless explicitly requested.

## Documentation

* `README.md` — GitHub project overview.
* `docs/technical-plan.md` — technical implementation plan.
* `docs/project-flow.md` — Mermaid-based architecture and project flow documentation.

Do not create additional documentation files unless explicitly requested.

## Development Rules

1. Inspect existing code before making changes.
2. Do not overwrite existing work without understanding it.
3. Implement one story/phase at a time.
4. Do not implement future features prematurely.
5. Keep implementations simple and understandable.
6. Follow existing project conventions.
7. Avoid unnecessary dependencies.
8. Avoid unnecessary design patterns.
9. Follow SOLID principles where appropriate.
10. Keep each service responsible for its own domain.

## Database Rules

Each service should own its own data.

Do not create a shared database between microservices unless explicitly approved.

Never hard-code database credentials.

Keep secrets and local configuration outside Git.

## API Rules

Use REST for normal synchronous communication.

Use Feign for appropriate internal synchronous service-to-service communication.

Use Kafka only when asynchronous communication is actually useful.

Use DTOs when communicating across service boundaries.

Do not expose external Survey API models directly to the frontend.

## Security Rules

* Passwords must always be securely hashed.
* Never store plaintext passwords.
* Never log passwords.
* Never log JWT tokens.
* Never commit secrets or credentials.
* JWT authentication will be implemented through Spring Security.

## Testing Rules

Every implemented feature should have appropriate automated tests.

At minimum, add unit tests for business logic and endpoint tests for important APIs.

Run tests after making changes.

Never claim that tests passed unless they were actually executed.

## Claude Code Rules

Before implementing a significant change:

1. Inspect the relevant code.
2. Explain the proposed approach.
3. Identify files that will be changed.
4. Wait for approval for major architectural changes.

When implementing a user story:

1. Understand the acceptance criteria.
2. Implement only that story.
3. Add/update tests.
4. Run the relevant tests.
5. Review the changes.
6. Report what was changed and what was tested.

Do not implement multiple unrelated stories at once.

Do not make destructive database, Git, Docker, or filesystem changes without asking.

## Scope Control

The current backlog consists of the approved Attrition Analyzer user stories.

Do not invent major new functionality.

If a requirement is unclear, ask before making a major architectural decision.

## Source of Truth

* For requirements: use the finalized Product Backlog.
* For architecture and flows: use `docs/project-flow.md` and the existing diagrams.
* For technical implementation planning: use `docs/technical-plan.md`.

If these sources conflict, stop and report the conflict instead of silently choosing one.

Do not invent requirements. Do not implement future stories prematurely.

## Git Safety

Never do the following without explicit approval:

* Force push
* Reset or rebase shared history
* Delete project files
* Untrack files
* Modify `.gitignore` destructively

Do not commit or push unless explicitly asked.

## Current Development Phase

We are currently setting up the project foundation.

Do not implement business functionality until the foundation is working.

The initial foundation consists of:

* Independent Spring Boot service projects
* Eureka Discovery Service
* Eureka client registration for every service
* API Gateway foundation
* Service ports/configuration
* Basic health checks
* Basic documentation

Verify that all services can start and register correctly before moving on.

After the foundation is verified, development will proceed story by story according to `docs/technical-plan.md`.

React
  ↓
API Gateway
  ↓
┌───────────────────────────────┐
│ Authentication                │
│ User Profile                  │
│ Employee                      │
│ Notification                  │
└───────────────────────────────┘
       ↓             ↓
     MySQL         Kafka
                     ↓
              Notification
                     
Employee → Survey API