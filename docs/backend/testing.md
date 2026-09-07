# Testing

What's tested in each service, how, and what each test class actually proves. This documents the
test suites as they exist today - it doesn't introduce new testing tools or change how tests run
(`mvn test`, same as before).

## Approach

Two layers, consistently across every service:

- **Unit tests** - a class in isolation, with its collaborators (repositories, Feign clients,
  other services) replaced by Mockito mocks. Fast, no Spring context, no database. These prove the
  class's own logic is correct: does a wrong password throw the right exception, does a bad salary
  bucket into the right band, does a duplicate Kafka event get ignored.
- **Integration tests** (`@SpringBootTest`) - the real Spring context wired together, backed by an
  in-memory H2 database (`MODE=MySQL`) instead of a mock repository. These prove the same behavior
  actually works once real persistence, real transactions, and real Spring wiring are involved -
  e.g. that a generated id or a `created_at` timestamp is actually populated by Hibernate, not just
  assumed by a mock.

Test method names read as a plain sentence about what's being proven - `shouldRejectWrongPassword`,
`shouldReturnEmptyWhenEmployeeNotFound` - rather than encoding the scenario into the name
(`login_withWrongPassword_throws401`). Each test sets up only what it needs, asserts on the one
behavior it's named after, and skips assertions on incidental detail that isn't the point of the
test (e.g. a create test checks the field that changed, not every field on the response).

## authentication-service

| Class | Type | What it proves |
|---|---|---|
| `AuthServiceTest` | Unit | `AuthService`'s own logic: login succeeds/fails for the right reasons (unknown email, wrong password), registration rejects duplicate emails, and the password saved is the hash - never the plaintext. |
| `AuthServiceIntegrationTest` | Integration | The same login/register behavior against a real `credentials` table - a seeded row can actually be logged into, and `registerCredential` actually persists a row you can read back. |
| `AuthControllerTest` | Unit (`@WebMvcTest`) | `/auth/login` and `/auth/logout` return the right HTTP status and body for valid input, invalid credentials, validation failures, and a missing token. |
| `InternalCredentialControllerTest` | Unit (`@WebMvcTest`) | `POST /internal/credentials` (the Feign-only registration endpoint) returns 201/409/400 correctly. |
| `JwtServiceTest` | Unit | Token round-trips (generate then parse gives back the same claims); expired, tampered, and malformed tokens are all rejected (empty `Optional`, never an exception). |
| `JwtAuthenticationFilterTest` | Unit | The filter only populates `SecurityContext` for a genuinely valid token - missing header, wrong scheme, invalid token, and a missing role claim all leave the request unauthenticated. |
| `SecurityChainIntegrationTest` | Integration | The real filter chain: `/auth/login` and `/actuator/health` are public, `/auth/logout` and an unknown path both require a valid JWT. |
| `CredentialTest` | Unit | The `Credential` entity's own behavior - the constructor sets what you pass it, `changePassword` replaces the hash. |

## user-profile-service

| Class | Type | What it proves |
|---|---|---|
| `ProfileServiceTest` | Unit | Reading/updating a profile by userId works, and both throw `ProfileNotFoundException` for an unknown user. |
| `ProfileServiceIntegrationTest` | Integration | The same behavior against a real `profiles` table - an update is actually persisted, not just returned. |
| `UserRegistrationServiceTest` | Unit | Registration calls Authentication Service (via a mocked Feign client) and persists a profile under the same `userId` it returns; duplicate emails (in this service's own DB, or reported back by Authentication) are rejected without leaving a half-created profile; a Feign 5xx becomes `AuthenticationServiceException`. |
| `UserRegistrationServiceIntegrationTest` | Integration | The same, against a real `profiles` table, with only the network call to Authentication Service mocked - proves the persisted row really has the `userId` Authentication returned. |
| `ProfileControllerTest` | Unit (`@WebMvcTest`) | `GET`/`PUT /users/me` return the right status for a valid token, a missing token, an unknown profile, and a validation failure - tokens are hand-built since this service never issues its own. |
| `UserRegistrationControllerTest` | Unit (`@WebMvcTest`) | `POST /users/register` returns 201/409/503/400 correctly. |
| `JwtServiceTest` | Unit | Same shape as authentication-service's, but verify-only - this service never signs a token, only checks one Authentication Service issued. |
| `JwtAuthenticationFilterTest` | Unit | Same coverage as authentication-service's filter test. |
| `SecurityChainIntegrationTest` | Integration | `/users/register` and `/actuator/health` are public, `/users/me` requires a valid JWT. |

## employee-service

| Class | Type | What it proves |
|---|---|---|
| `EmployeeServiceTest` | Unit | Every attrition-analysis dimension (department, job role, compensation band, gender, overtime, promotion recency) groups and calculates rates correctly, including edge cases (blank department, null salary, null years-since-promotion all bucket as "Unknown" rather than crashing); a Survey API 404 becomes an empty result, a 500 becomes `SurveyApiException`; flagging an employee builds and publishes the right event, and a Kafka publish failure propagates rather than being swallowed. |
| `EmployeeControllerTest` | Unit (`@WebMvcTest`) | Every endpoint (`/employees`, `/employees/{id}`, `/employees/{id}/flag`, and all six `/employees/analysis/*` routes) returns the right status for the happy path and its real failure modes (404, 400, 401, 503). |
| `EmployeeMapperTest` | Unit | The Survey API response shape maps to `EmployeeDto` field-for-field. |
| `SurveyApiClientIT` | Integration | The Feign client against a **real** Survey API instance - fetching all employees, filtering by department, and a 404 for an unknown id. Named `*IT` so a plain `mvn test` skips it (no Survey API running in CI); run explicitly when the Survey API container is up. |

## notification-service

| Class | Type | What it proves |
|---|---|---|
| `NotificationServiceTest` | Unit | Creating a notification (directly, or from a Kafka event) saves the right fields, including the sender's name; a duplicate Kafka `eventId` is a no-op; the notification list isn't filtered by creator; a missing sender name falls back to their email; marking read and deleting both work, and both throw `NotificationNotFoundException` for an unknown id. |
| `NotificationServiceIntegrationTest` | Integration | The same behavior against a real `notifications` table - a created notification is actually persisted with a generated id, one HR user's notification is visible to another, and a read/delete is actually remembered after reload. |
| `NotificationControllerTest` | Unit (`@WebMvcTest`) | `POST/GET /notifications`, `PATCH /notifications/{id}/read`, and `DELETE /notifications/{id}` all return the right status for valid input, a missing token, a validation failure, and an unknown id. |
| `EmployeeFlaggedEventListenerTest` | Unit | The Kafka listener creates a notification per event, silently ignores the specific duplicate-`event_id` constraint violation (expected under Kafka's at-least-once delivery), and re-throws any other database or unexpected failure rather than swallowing it. |
| `JwtServiceTest` | Unit | `extractEmail` reads the email claim from a valid token and throws `UnauthenticatedException` for a malformed or wrongly-signed one. |

## api-gateway

| Class | Type | What it proves |
|---|---|---|
| `JwtServiceTest` | Unit | Same shape as the other services' - verify-only, tokens hand-built. |
| `JwtAuthenticationFilterTest` | Unit | Same coverage as the other services' filter test. |
| `SecurityChainIntegrationTest` | Integration | The actual routing rules: `/actuator/health`, `/auth/login`, and `/users/register` are reachable without a token (asserted as "not 401", since the downstream services aren't running in this test); an unmapped path is rejected without a token, rejected with an invalid or expired token, and reaches a 404 (not 401) with a valid one - proving the auth layer, not routing, is what's being tested. |

`discovery-service` has no tests of its own - it's Eureka's stock server with no custom business
logic to test.
