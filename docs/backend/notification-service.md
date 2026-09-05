# Notification Service

## Purpose

Owns notifications: creating them (both directly and from the Kafka flow), listing a user's own notifications, and deleting them. This is the consumer side of the [`employee.flagged` Kafka flow](kafka.md) — see that doc for the full producer → broker → consumer picture; this doc focuses on what happens once an event (or a direct API call) reaches this service.

## Architecture / packages

| Package | Contents |
|---|---|
| `controller` | `NotificationController` |
| `service` | `NotificationService` |
| `entity` | `Notification` |
| `repository` | `NotificationRepository` |
| `event` | `EmployeeFlaggedEvent` (this service's own copy), `EmployeeFlaggedEventListener` (`@KafkaListener`) |
| `dto` | `CreateNotificationRequest`, `NotificationDto`, `ErrorResponse` |
| `security` | `JwtService` — verify-only, used to read the caller's email off the token |
| `exception` | `NotificationNotFoundException`, `UnauthenticatedException`, `GlobalExceptionHandler` |

Like Employee Service, there's **no `SecurityConfig`/Spring Security dependency** here — the Gateway enforces that every `/notifications/**` route requires a valid JWT; this service just reads the caller's identity out of the `Authorization` header it's forwarded.

## Endpoints

| Method | Path | Auth (Gateway) | Purpose |
|---|---|---|---|
| POST | `/notifications` | Yes | Direct create: `{employeeId, employeeName, department, comment}`, owner = caller's email. Not currently called by the frontend UI — the UI's only path to a notification is flagging an employee, which goes through Kafka instead |
| GET | `/notifications` | Yes | Returns **only the caller's own** notifications (`findByHrUserEmailOrderByCreatedAtDesc`), newest first |
| DELETE | `/notifications/{id}` | Yes | Deletes a notification — but only if `notification.getHrUserEmail()` matches the caller; otherwise responds as if it doesn't exist (`NotificationNotFoundException`), rather than leaking that a different user's notification exists at that ID |

Every endpoint extracts the caller's email itself (`NotificationController.currentUserEmail`, via the local `JwtService`, reading the `Authorization` header directly) rather than relying on a populated Spring Security principal — this service has no security filter chain to populate one.

## Two ways a notification is created

```java
// 1. Direct API call
public NotificationDto createNotification(CreateNotificationRequest request, String hrUserEmail)

// 2. From a consumed Kafka event
public boolean createFromEvent(EmployeeFlaggedEvent event)
```

Both build the same `Notification` entity; the event-based path additionally sets `eventId` (the Kafka idempotency key — see below) and, on a detected duplicate, returns `false` instead of throwing, so `EmployeeFlaggedEventListener` can log-and-ignore rather than fail the whole consumer.

## Kafka consumption

`EmployeeFlaggedEventListener.onEmployeeFlagged` is the `@KafkaListener(topics = "${notification.kafka.topic}", groupId = "notification-service")` method. It calls `NotificationService.createFromEvent`, then:

- If `createFromEvent` returns `false` (an `existsByEventId` hit), logs an info line and does nothing further.
- If the underlying `INSERT` still races into the unique `event_id` constraint (concurrent duplicate delivery slipping past the `existsByEventId` pre-check), the listener catches `DataIntegrityViolationException`, confirms the violation message mentions `event_id`, and treats it the same way — a safe no-op, not a failure.
- Any other `DataIntegrityViolationException` (a real, unrelated data problem) is **rethrown**, so Spring Kafka's container error handler retries/logs it as a genuine failure rather than silently swallowing it.

See [kafka.md](kafka.md) for the event schema, topic name, consumer group, and why `eventId` is the idempotency key.

## Data model

`Notification` (table `notifications` in `notification_db`): `id` (identity PK), `employee_id`, `employee_name`, `department` (all snapshotted at creation time — not looked up live from Employee Service later), `hr_user_email` (the owner, populated from the caller's JWT `email` claim), `comment`, `event_id` (nullable, unique — null for directly-created notifications, populated for event-sourced ones), `created_at`. See [database.md](database.md).

`JwtService.extractEmail` reads `claims.get("email", String.class)`, so `hr_user_email` holds the caller's actual email address on both write paths — the direct `POST /notifications` endpoint and a consumed Kafka event (employee-service's identically-fixed `extractEmail`, see [employee-service.md](employee-service.md)). Verified end to end: a notification created directly, and one created via the flag → Kafka flow, both stored the real email; a second user attempting to `DELETE` the first user's notification got `404` (not found, rather than revealing it exists), and the owner's own `DELETE` succeeded. Rows created before this fix retain the old UUID value in `hr_user_email`, since existing data isn't retroactively migrated — only newly-created notifications are affected by the fix.

## Security

Own `JwtService` (verify-only, same shared secret as every other verifier), used purely to identify the caller for the three endpoints above — there's no separate authorization logic here beyond "you can only see/delete your own notifications," enforced in `NotificationService` itself (`deleteNotification` checks `hrUserEmail` equality; `getNotificationsForUser` filters by it directly in the repository query).

## Communication with other services

- **Inbound:** reached through the Gateway (`/notifications/**`); consumes Kafka events published by employee-service (no direct REST call between the two services for this flow).
- Never calls another service itself.

## Docker

Standard backend Dockerfile (see [docker.md](docker.md)). `docker-compose.yml` overrides its datasource URL to `mysql-db:3306/notification_db` and sets `-Dspring.kafka.bootstrap-servers=kafka:9092`; waits for both `mysql-db` and `kafka` to be healthy before starting.

## Testing

| Test | Covers |
|---|---|
| `NotificationServiceApplicationTests` | Context loads |
| `NotificationControllerTest` | Endpoint behavior (create/list/delete, ownership checks) |
| `NotificationServiceTest` | Business logic, including the `createFromEvent` duplicate-handling path |
| `JwtServiceTest` | Token verification |
| `EmployeeFlaggedEventListenerTest` | Consumer behavior, including both duplicate-handling paths (pre-check hit and DB-constraint race) |
