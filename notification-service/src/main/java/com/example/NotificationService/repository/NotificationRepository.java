package com.example.NotificationService.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.NotificationService.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Notifications are visible to every HR user, not just their creator -
    // see NotificationDto.senderEmail for how the creator is still surfaced.
    List<Notification> findAllByOrderByCreatedAtDesc();

    boolean existsByEventId(UUID eventId);
}
