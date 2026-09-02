package com.example.NotificationService.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.NotificationService.dto.NotificationDto;
import com.example.NotificationService.entity.Notification;
import com.example.NotificationService.exception.NotificationNotFoundException;
import com.example.NotificationService.repository.NotificationRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<NotificationDto> getNotificationsForUser(String hrUserEmail) {
        List<Notification> notifications = notificationRepository.findByHrUserEmailOrderByCreatedAtDesc(hrUserEmail);

        List<NotificationDto> result = new ArrayList<>();
        for (Notification notification : notifications) {
            result.add(toDto(notification));
        }
        return result;
    }

    public void deleteNotification(Long id, String hrUserEmail) {
        Notification notification = notificationRepository.findById(id).orElse(null);
        if (notification == null || !notification.getHrUserEmail().equals(hrUserEmail)) {
            throw new NotificationNotFoundException();
        }
        notificationRepository.delete(notification);
    }

    private NotificationDto toDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getEmployeeId(),
                notification.getEmployeeName(),
                notification.getDepartment(),
                notification.getComment(),
                notification.getCreatedAt());
    }
}
