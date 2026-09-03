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

    @Column(nullable = false, length = 1000)
    private String comment;

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
            String comment) {
        this(employeeId, employeeName, department, hrUserEmail, comment, null);
    }

    public Notification(String employeeId, String employeeName, String department, String hrUserEmail,
            String comment, UUID eventId) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.department = department;
        this.hrUserEmail = hrUserEmail;
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

    public String getComment() {
        return comment;
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
