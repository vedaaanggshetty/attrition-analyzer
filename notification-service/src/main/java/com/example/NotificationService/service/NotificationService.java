package com.example.NotificationService.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.NotificationService.dto.CreateNotificationRequest;
import com.example.NotificationService.dto.NotificationDto;
import com.example.NotificationService.entity.Notification;
import com.example.NotificationService.event.EmployeeFlaggedEvent;
import com.example.NotificationService.exception.NotificationNotFoundException;
import com.example.NotificationService.repository.NotificationRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public NotificationDto createNotification(CreateNotificationRequest request, String hrUserEmail) {
        Notification notification = new Notification(
                request.employeeId(),
                request.employeeName(),
                request.department(),
                hrUserEmail,
                request.comment());
        notification = notificationRepository.save(notification);
        return toDto(notification);
    }

    /**
     * Creates a Notification from a consumed EmployeeFlaggedEvent. Returns
     * false (no-op) if a notification for this eventId already exists, so the
     * listener can tell duplicate deliveries apart from new ones.
     */
    public boolean createFromEvent(EmployeeFlaggedEvent event) {
        if (notificationRepository.existsByEventId(event.eventId())) {
            return false;
        }

        Notification notification = new Notification(
                event.employeeId(),
                event.employeeName(),
                event.department(),
                event.hrUserEmail(),
                event.comment(),
                event.eventId());
        notificationRepository.save(notification);
        return true;
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
