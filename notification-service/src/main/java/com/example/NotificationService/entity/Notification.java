package com.example.NotificationService.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
        // required by JPA/Hibernate
    }

    public Notification(String employeeId, String employeeName, String department, String hrUserEmail,
            String comment) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.department = department;
        this.hrUserEmail = hrUserEmail;
        this.comment = comment;
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

    public Instant getCreatedAt() {
        return createdAt;
    }
}
