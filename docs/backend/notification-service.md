# Notification Service

## Purpose

Owns notifications: creating them (both directly and from the Kafka flow), listing a user's own notifications, and deleting them. This is the consumer side of the [`employee.flagged` Kafka flow](kafka.md) — see that doc for the full producer → broker → consumer picture; this doc focuses on what happens once an event (or a direct API call) reaches this service.

**Why notifications are their own service instead of living inside Employee Service (which is what triggers them):** ownership and lifecycle are genuinely different. A notification, once created, is a persistent HR-facing record a user views and deletes over time — it needs a real database and its own list/delete API. Employee Service, by contrast, is intentionally stateless and has no database at all (see [employee-service.md](employee-service.md)). Putting notification storage there would force Employee Service to suddenly own persistent data, contradicting its whole design. Keeping them separate, connected only by a Kafka event, means Employee Service can stay a pure stateless proxy while this service can be the one place that owns the full notification lifecycle.

**What this service owns:** the `notifications` table, and both ways a notification can come into existence (direct API call, or consumed Kafka event). **What it explicitly does not own:** deciding *when* an employee should be flagged, or any employee data beyond the snapshot (name/department) captured at creation time — it never calls Employee Service or the Survey API itself.

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

This service's data (who owns which notification) is directly tied to *who the caller is*, unlike Employee Service's data (which is the same for every caller). That's exactly why, unlike Employee Service, this service's own logic (not just the Gateway) actively uses the caller's identity for every operation — ownership filtering happens in `NotificationService`, not just as a route-level allow/deny at the Gateway.

## Endpoints

| Method | Path | Auth (Gateway) | Purpose |
|---|---|---|---|
| POST | `/notifications` | Yes | Direct create: `{employeeId, employeeName, department, comment}`, owner = caller's email. Not currently called by the frontend UI — the UI's only path to a notification is flagging an employee, which goes through Kafka instead |
| GET | `/notifications` | Yes | Returns **only the caller's own** notifications (`findByHrUserEmailOrderByCreatedAtDesc`), newest first |
| DELETE | `/notifications/{id}` | Yes | Deletes a notification — but only if `notification.getHrUserEmail()` matches the caller; otherwise responds as if it doesn't exist (`NotificationNotFoundException`), rather than leaking that a different user's notification exists at that ID |

Every endpoint extracts the caller's email itself (`NotificationController.currentUserEmail`, via the local `JwtService`, reading the `Authorization` header directly) rather than relying on a populated Spring Security principal — this service has no security filter chain to populate one.

**`GET /notifications`, end to end:**

```
Frontend → GET /notifications  (Authorization: Bearer <token>)
  → Gateway: verifies JWT (protected route) → routes to notification-service
    → NotificationController.getMyNotifications
      → currentUserEmail(authorization) — extracts "email" claim via JwtService
        — missing/malformed Authorization header → UnauthenticatedException (401)
      → NotificationService.getNotificationsForUser(email)
        → NotificationRepository.findByHrUserEmailOrderByCreatedAtDesc(email)
  ← 200 [NotificationDto, ...]   (only this caller's own rows, newest first)
```

**`DELETE /notifications/{id}`, and why it returns 404 rather than 403 for someone else's notification:** `NotificationService.deleteNotification` loads the row by ID, then checks `notification.getHrUserEmail().equals(hrUserEmail)` — if it's missing *or* owned by someone else, both cases throw the same `NotificationNotFoundException` (`404`). Returning `403 Forbidden` instead would confirm to the caller that a notification with that ID exists and belongs to someone else — a minor information leak this design avoids on purpose, the same "don't reveal what you can't have" principle used by Authentication's login error (see [authentication-service.md](authentication-service.md)).

## Two ways a notification is created

```java
// 1. Direct API call
public NotificationDto createNotification(CreateNotificationRequest request, String hrUserEmail)

// 2. From a consumed Kafka event
public boolean createFromEvent(EmployeeFlaggedEvent event)
```

Both build the same `Notification` entity; the event-based path additionally sets `eventId` (the Kafka idempotency key — see below) and, on a detected duplicate, returns `false` instead of throwing, so `EmployeeFlaggedEventListener` can log-and-ignore rather than fail the whole consumer.

Having two creation paths that converge on the same entity, rather than two unrelated code paths, is what keeps `GET /notifications` and `DELETE /notifications/{id}` simple — they don't need to know or care whether a given notification came from a direct API call or a Kafka event; both look identical once persisted, except for whether `event_id` is populated.

## Kafka consumption

`EmployeeFlaggedEventListener.onEmployeeFlagged` is the `@KafkaListener(topics = "${notification.kafka.topic}", groupId = "notification-service")` method. It calls `NotificationService.createFromEvent`, then:

- If `createFromEvent` returns `false` (an `existsByEventId` hit), logs an info line and does nothing further.
- If the underlying `INSERT` still races into the unique `event_id` constraint (concurrent duplicate delivery slipping past the `existsByEventId` pre-check), the listener catches `DataIntegrityViolationException`, confirms the violation message mentions `event_id`, and treats it the same way — a safe no-op, not a failure.
- Any other `DataIntegrityViolationException` (a real, unrelated data problem) is **rethrown**, so Spring Kafka's container error handler retries/logs it as a genuine failure rather than silently swallowing it.

See [kafka.md](kafka.md) for the event schema, topic name, consumer group, and why `eventId` is the idempotency key.

**Why check `existsByEventId` first *and* also rely on the DB constraint, instead of just one:** they guard against different situations. The `existsByEventId` check is the fast, common-case path — cheap, and correct for the overwhelming majority of deliveries. But Kafka's at-least-once delivery guarantee means two consumer threads (or a redelivery racing a still-in-flight first delivery) could both pass that check before either has inserted — the check alone has a race window. The unique DB constraint is what actually closes that window: even if both "think" they're first, only one `INSERT` can succeed, and the loser's constraint violation is caught and treated as the safe, expected outcome it is. Relying on the constraint *alone* (skipping the pre-check) would also be correct, just slower on average — the pre-check exists purely as an optimization, not for correctness.

## Data model

`Notification` (table `notifications` in `notification_db`): `id` (identity PK), `employee_id`, `employee_name`, `department` (all snapshotted at creation time — not looked up live from Employee Service later), `hr_user_email` (the owner, populated from the caller's JWT `email` claim), `comment`, `event_id` (nullable, unique — null for directly-created notifications, populated for event-sourced ones), `created_at`. See [database.md](database.md).

`JwtService.extractEmail` reads `claims.get("email", String.class)`, so `hr_user_email` holds the caller's actual email address on both write paths — the direct `POST /notifications` endpoint and a consumed Kafka event (employee-service's identically-fixed `extractEmail`, see [employee-service.md](employee-service.md)). Verified end to end: a notification created directly, and one created via the flag → Kafka flow, both stored the real email; a second user attempting to `DELETE` the first user's notification got `404` (not found, rather than revealing it exists), and the owner's own `DELETE` succeeded. Rows created before this fix retain the old UUID value in `hr_user_email`, since existing data isn't retroactively migrated — only newly-created notifications are affected by the fix.

**Why `employee_id`/`employee_name`/`department` are snapshotted here rather than looked up live from Employee Service when displaying a notification:** a notification is meant to be a historical record of "this employee was flagged, with this comment, at this time" — if the employee's department later changed, or if the Survey API's data about them changed or became temporarily unreachable, the notification should still show what was true *at the moment it was created*, not silently change or fail to load later. Snapshotting also avoids an extra cross-service call (Employee Service, which itself calls the external Survey API) every time `GET /notifications` is rendered.

## Security

Own `JwtService` (verify-only, same shared secret as every other verifier), used purely to identify the caller for the three endpoints above — there's no separate authorization logic here beyond "you can only see/delete your own notifications," enforced in `NotificationService` itself (`deleteNotification` checks `hrUserEmail` equality; `getNotificationsForUser` filters by it directly in the repository query).

## Communication with other services

- **Inbound:** reached through the Gateway (`/notifications/**`); consumes Kafka events published by employee-service (no direct REST call between the two services for this flow).
- Never calls another service itself.

Structurally, this makes notification-service another "leaf" like authentication-service (see [authentication-service.md](authentication-service.md)) — it receives work from two directions (the Gateway, and Kafka) but never initiates a call outward to anyone else. Combined with the fact it consumes rather than polls (it doesn't ask "any new flags?" — Kafka pushes the event to it), it can react to Employee Service's activity without either service ever needing to know the other's network address at the business-logic level (only Kafka's).

## Docker

Standard backend Dockerfile (see [docker.md](docker.md)). `docker-compose.yml` overrides its datasource URL to `mysql-db:3306/notification_db` and sets `-Dspring.kafka.bootstrap-servers=kafka:9092`; waits for both `mysql-db` and `kafka` to be healthy before starting.

Waiting on *both* health checks (not just one) matters here specifically because this service genuinely needs both dependencies working from the moment it starts: it needs the database ready to persist notifications, and it needs Kafka ready so its `@KafkaListener` can subscribe to `employee.flagged` — starting before either is ready would mean either datasource connection failures or the consumer failing to establish its subscription.

## Testing

| Test | Covers |
|---|---|
| `NotificationServiceApplicationTests` | Context loads |
| `NotificationControllerTest` | Endpoint behavior (create/list/delete, ownership checks) |
| `NotificationServiceTest` | Business logic, including the `createFromEvent` duplicate-handling path |
| `JwtServiceTest` | Token verification |
| `EmployeeFlaggedEventListenerTest` | Consumer behavior, including both duplicate-handling paths (pre-check hit and DB-constraint race) |

`EmployeeFlaggedEventListenerTest` is the one most worth being able to explain in detail: it's what actually proves the idempotency guarantee described above holds — not just "a duplicate event doesn't crash the consumer," but specifically that both the fast pre-check path *and* the DB-constraint-race path are handled as safe no-ops, while an unrelated `DataIntegrityViolationException` (simulating a genuine data problem, not a duplicate) is correctly rethrown rather than silently swallowed. `NotificationControllerTest` covers the ownership behavior (a non-owner's delete correctly 404s) described above under Endpoints.

## How to explain this service in a presentation

"Notification Service owns the notification lifecycle — creating, listing, and deleting. There are two ways a notification gets created: a direct POST /notifications call, and the more important one, consuming a Kafka event published when an HR user flags an employee. It's the consumer half of that asynchronous flow, so it never talks to Employee Service directly — it just listens on the employee.flagged topic. Every notification is scoped to the HR user who owns it, identified by the email claim in their JWT, and both listing and deleting enforce that ownership — deleting someone else's notification returns 404, not 403, so the API never confirms that a notification you don't own actually exists. Duplicate Kafka deliveries, which the platform can produce under normal at-least-once semantics, are made safe by a unique constraint on the event's ID — a duplicate delivery is detected and ignored rather than creating a second notification."
