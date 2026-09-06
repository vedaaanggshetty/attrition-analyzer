package com.example.NotificationService.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity mapping to the {@code notifications} table in {@code notification_db}.
 *
 * Employee details are stored directly on the notification (not looked up live
 * from Employee Service) because they're captured once at creation time, per
 * the project's documented notification design.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    @Column(name = "employee_name", nullable = false)
    private String employeeName;

    @Column(nullable = false)
    private String department;

    // Owning HR user, identified by email - Authentication Service's JWT only
    // carries an email claim today, no user_id. Switch this to a real user id
    // once Authentication/User Profile are merged and the JWT carries one.
    @Column(name = "hr_user_email", nullable = false)
    private String hrUserEmail;

    // Display name of the HR user who sent this, captured once at creation
    // time (same reasoning as employeeName/department above) since the JWT
    // itself doesn't carry a name claim. Nullable - notifications created
    // before this field existed, or without a name available, fall back to
    // showing hrUserEmail in the UI instead.
    @Column(name = "hr_user_name")
    private String hrUserName;

    @Column(nullable = false, length = 1000)
    private String comment;

    // Notifications are shared across all HR users (not private to their
    // creator) - "read" tracks whether any HR user has reviewed it yet, as a
    // single shared flag on the notification itself rather than per-user state.
    // Mapped to "is_read", not "read" - READ is a reserved word in MySQL and
    // produces a SQL syntax error if used unquoted as a column name.
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    // Idempotency key for notifications created from a Kafka EmployeeFlaggedEvent.
    // Null for notifications created via the direct POST /notifications flow,
    // which has no event to dedupe against.
    @Column(name = "event_id", unique = true, columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID eventId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
        // required by JPA/Hibernate
    }

    public Notification(String employeeId, String employeeName, String department, String hrUserEmail,
            String hrUserName, String comment) {
        this(employeeId, employeeName, department, hrUserEmail, hrUserName, comment, null);
    }

    public Notification(String employeeId, String employeeName, String department, String hrUserEmail,
            String hrUserName, String comment, UUID eventId) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.department = department;
        this.hrUserEmail = hrUserEmail;
        this.hrUserName = hrUserName;
        this.comment = comment;
        this.eventId = eventId;
    }

    public Long getId() {
        return id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getDepartment() {
        return department;
    }

    public String getHrUserEmail() {
        return hrUserEmail;
    }

    public String getHrUserName() {
        return hrUserName;
    }

    public String getComment() {
        return comment;
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isRead() {
        return read;
    }

    public void markRead() {
        this.read = true;
    }
}
