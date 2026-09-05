# Kafka: the `employee.flagged` Flow

Kafka is used for exactly one thing in this project: decoupling "an HR user flagged an employee" from "a notification exists for it." Everywhere else in the system, services talk synchronously (REST via the Gateway, Feign service-to-service) — see [employee-service.md](employee-service.md) and [notification-service.md](notification-service.md) for those endpoints. The frontend never talks to Kafka, or even knows it exists — see [frontend.md](../frontend/frontend.md).

## Why Kafka here and nowhere else

Flagging an employee is a "fire this off and let another service deal with it" action from the caller's point of view — the HR user doesn't need to wait for a notification row to be inserted, just confirmation that the flag was recorded. Every other cross-service call in this project (registration's Feign call to Authentication, the Gateway's routing) is a request that needs an immediate, synchronous answer to return to the client, so REST/Feign is used there instead — Kafka is reserved for this one asynchronous, fire-and-forget case.

**The concrete benefit of decoupling this specific action:** without Kafka, `POST /employees/{id}/flag` would need to call notification-service synchronously (REST or Feign) and wait for it to finish inserting a row before responding. That would mean employee-service's flag endpoint could fail *because notification-service happened to be down or slow*, even though the "fact" being recorded (an employee was flagged, by whom, with what comment) has nothing to do with notification-service's availability. With Kafka, the event is durably published and employee-service's job is done; if notification-service is briefly down, the event just waits on the topic until it comes back and consumes it — no data is lost, and employee-service's endpoint isn't coupled to notification-service's uptime. This is the core argument for choosing asynchronous messaging over a synchronous call for this one interaction, and the reason it doesn't apply to, say, registration (where the frontend *does* need to know immediately whether the credential was actually created, so waiting synchronously is correct there).

## The flow end to end

```
HR (frontend)
  │  POST /employees/{id}/flag  {comment}
  ▼
API Gateway :8080  (JWT verified)
  ▼
Employee Service :8083
  │  looks up the employee via the Survey API (for name/department)
  │  builds an EmployeeFlaggedEvent
  ▼
Kafka topic: employee.flagged
  ▼
Notification Service :8084  (consumer group: notification-service)
  │  INSERT INTO notifications (idempotent on event_id)
  ▼
notification_db
  ▲
  │  GET /notifications
HR (frontend, later)
```

1. HR calls `POST /employees/{id}/flag` with `{ "comment": "..." }` through the Gateway.
2. `EmployeeController.flagEmployee` (employee-service) extracts the caller's identity from the JWT and calls `EmployeeService.flagEmployee`.
3. `EmployeeService` re-uses `getEmployeeById` (the same Survey API lookup used by `GET /employees/{id}`) to get the employee's name/department, then builds an `EmployeeFlaggedEvent` and hands it to `EmployeeFlaggedEventProducer`.
4. `EmployeeFlaggedEventProducer.publish` sends the event to Kafka via `KafkaTemplate`, keyed by `employeeId`, and calls `.get()` on the send future — this makes the publish **synchronous** from the controller's perspective, so a broken/unreachable broker surfaces as a thrown `EventPublicationException` (→ an HTTP error) instead of the request succeeding while the event silently vanishes.
5. The controller returns `202 Accepted` with the event body once Kafka has acknowledged the send.
6. `EmployeeFlaggedEventListener` (notification-service), subscribed via `@KafkaListener`, consumes the event and calls `NotificationService.createFromEvent`.
7. `createFromEvent` checks `existsByEventId` first (fast path), then inserts a `Notification` row. The `notifications.event_id` column also has a **unique DB constraint** — the true idempotency guard — so even a duplicate delivery that races past the `existsByEventId` check fails safely on that constraint instead of double-inserting; the listener catches that specific `DataIntegrityViolationException` and treats it as an ignorable duplicate (any other constraint violation is rethrown, since it's a real data/schema problem, not a duplicate).
8. HR later calls `GET /notifications` and sees the new row.

**What can fail at each step, and what the caller sees:**

| Step | Failure | Result |
|---|---|---|
| 3 | Employee ID doesn't exist (Survey API 404) | `EmployeeService.flagEmployee` returns `Optional.empty()`, controller responds `404 Not Found` — no event is ever published |
| 4 | Kafka broker unreachable/timeout | `.get()` throws, wrapped as `EventPublicationException` → `503 Service Unavailable` — the HR user sees a clear failure, not a false success |
| 6–7 | notification-service is down when the event is published | No failure visible to HR at all — the event sits durably on the topic until notification-service is back up and its consumer catches up; nothing is lost |
| 7 | Duplicate delivery of the same event (Kafka's at-least-once guarantee) | Silently ignored (see Idempotency below) — no duplicate notification, no error |

That third row is the practical payoff of using Kafka here: a downstream outage in notification-service never turns into a failed flag action for the HR user, and never loses the flag either.

## Event schema

`EmployeeFlaggedEvent` is defined **twice** — once in employee-service's `event` package (producer) and once in notification-service's `event` package (consumer) — as plain Java records with identical fields, not shared via a common library. This follows the same "no shared library, each service owns its own contract" pattern used for every other cross-service DTO in this project.

```json
{
  "eventId": "9450f888-92cc-4ecf-b77f-6ff8bd5fa87c",
  "employeeId": "3012-1A41",
  "employeeName": "Leonelle Simco",
  "department": "Sales",
  "comment": "Flight risk - discussed comp in 1:1",
  "hrUserEmail": "...",
  "flaggedAt": "2026-09-05T12:09:24.977605625Z"
}
```

| Field | Purpose |
|---|---|
| `eventId` | Idempotency key (see below). A fresh `UUID.randomUUID()` per flag action, generated by employee-service. |
| `employeeId` / `employeeName` / `department` | Snapshotted at flag time from the Survey API response — not looked up again later, so a notification still shows the employee's details even if the Survey API's data changes afterward. |
| `comment` | The HR user's free-text reason for flagging. |
| `hrUserEmail` | Identifies who flagged the employee. |
| `flaggedAt` | Server timestamp at the moment of flagging (not the same as `notifications.created_at`, which is when the row was inserted — normally milliseconds apart). |

**Why the schema is duplicated as two separate records instead of a shared library:** this project deliberately has no shared code module between services (every service is an independent Maven project — see the root `README.md`'s Repository Structure), so that each service can be built, tested, and deployed completely independently, with no version-coupling between them. The cost of that choice is exactly what's visible here: if the event's shape needs a new field, both copies must be updated in lockstep, and nothing enforces that automatically at compile time (a schema-registry-based approach, e.g. Avro with Confluent Schema Registry, would catch a mismatch — this project uses plain JSON records instead, favoring simplicity over that safety net, appropriate at this project's scale).

| Setting | Value | Configured in |
|---|---|---|
| Topic | `employee.flagged` | `notification.kafka.topic` property, both services |
| Producer | employee-service | `EmployeeFlaggedEventProducer` (`KafkaTemplate<String, EmployeeFlaggedEvent>`) |
| Consumer | notification-service | `EmployeeFlaggedEventListener` (`@KafkaListener`) |
| Consumer group | `notification-service` | `spring.kafka.consumer.group-id` |
| Key | `employeeId` | set on `kafkaTemplate.send(topic, event.employeeId(), event)` |
| Serialization | JSON, no type headers | producer sets `spring.json.add.type.headers=false`; consumer forces every message to `EmployeeFlaggedEvent` via `spring.json.value.default.type`, since the consumer's copy of the class lives in a different package than the producer's and can't trust a type header naming the producer's class |
| Bootstrap servers (Docker) | `kafka:9092` | `KAFKA_BOOTSTRAP_SERVERS` env override in `docker-compose.yml` |
| Bootstrap servers (local default) | `localhost:9092` | `application.properties` default |

No manual topic creation is needed — the broker auto-creates `employee.flagged` on first publish (default Kafka behavior; nothing in this project disables auto-topic-creation).

**Why the message is keyed by `employeeId`:** Kafka guarantees ordering only *within* a partition, and a topic's messages are distributed across partitions based on their key (same key → same partition, always). Keying by `employeeId` means every flag event for the *same* employee is guaranteed to be processed in the order they were published, which matters if an employee could plausibly be flagged more than once — without a key, Kafka would distribute messages round-robin across partitions with no ordering guarantee between them, and two flags for the same employee could theoretically be processed out of order.

**Why `notification-service` as the consumer group name matters:** Kafka delivers each message to one consumer *per consumer group*, not to every individual consumer instance. Naming the group after the service (rather than a generic default) is what would let this service safely run multiple instances in the future — each message would still only be processed once across all of them (load-shared, not duplicated), rather than every instance separately consuming every message.

## Idempotency / `eventId`

`eventId` exists specifically so that **at-least-once delivery** (Kafka's default guarantee — a rebalance, retry, or redelivery can hand the consumer the same message twice) never produces two notifications for one flag action:

- `notifications.event_id` is a nullable, **unique** column (nullable because notifications created via the direct `POST /notifications` endpoint have no event to key off).
- `NotificationService.createFromEvent` first checks `existsByEventId(event.eventId())` and no-ops if it's already present (the fast, common-case path).
- If two deliveries race past that check concurrently, the second `INSERT` violates the unique constraint; `EmployeeFlaggedEventListener` catches `DataIntegrityViolationException`, confirms the violation message mentions `event_id`, and treats it as a harmless duplicate (logs and returns) rather than failing the whole consumer.

**Why "at-least-once" delivery happens at all, in plain terms:** Kafka's consumer offset (the "how far have I read" bookmark) is committed *after* a message is processed, not before. If the consumer crashes or is rebalanced between finishing the processing and committing the offset, the same message gets redelivered on restart — better to risk a duplicate than to risk silently losing a message (the alternative, committing the offset *before* processing, could lose messages on a crash instead). This is a deliberate, standard trade-off in Kafka's design, and `eventId` plus the unique constraint is this project's answer to the "at-least-once can duplicate" half of that trade-off.

## Not a frontend concept

The frontend never sees Kafka, the topic name, or `eventId` in its own code — it only calls `POST /employees/{id}/flag` and, separately, `GET /notifications`. See [frontend.md](../frontend/frontend.md) for how those two calls appear from the UI's perspective.

This is exactly the point of putting Kafka behind two ordinary-looking REST endpoints: the frontend's job is simple ("call this to flag, call that to list notifications"), and the asynchronous machinery in between is an implementation detail of the backend, free to change (a different broker, a different idempotency scheme, even switching to a synchronous call if requirements changed) without the frontend ever needing to know.

## How to explain this flow in a presentation

"When an HR user flags an employee, Employee Service doesn't create the notification directly — it publishes an event to a Kafka topic called employee.flagged and returns immediately with a 202 Accepted, because the actual notification is created asynchronously. Notification Service consumes that topic and inserts the notification. We use Kafka specifically for this one interaction because it decouples the two services — if Notification Service were briefly down, the flag would still succeed and the event would just wait on the topic until it's back up, instead of failing the whole flag action. Every other cross-service call in the project, like registration calling Authentication, is synchronous REST or Feign, because those need an immediate answer. The one tricky part is that Kafka can redeliver the same event more than once — that's a normal part of its at-least-once guarantee — so every event carries a unique event ID, and a database uniqueness constraint on that ID is what actually guarantees a duplicate delivery never creates two notifications."
