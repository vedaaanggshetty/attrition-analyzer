package com.example.NotificationService.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.NotificationService.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByHrUserEmailOrderByCreatedAtDesc(String hrUserEmail);
}
