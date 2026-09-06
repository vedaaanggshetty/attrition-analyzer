package com.example.NotificationService.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
                request.hrUserName(),
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
                event.hrUserName(),
                event.comment(),
                event.eventId());
        notificationRepository.save(notification);
        return true;
    }

    /**
     * Notifications are shared across every HR user - the caller's identity is
     * no longer used to filter this list, only to identify who creates or
     * reviews a notification (see {@link #createNotification} and
     * {@link #markAsRead}).
     */
    public List<NotificationDto> getAllNotifications() {
        List<Notification> notifications = notificationRepository.findAllByOrderByCreatedAtDesc();

        List<NotificationDto> result = new ArrayList<>();
        for (Notification notification : notifications) {
            result.add(toDto(notification));
        }
        return result;
    }

    /**
     * Marks a notification as reviewed. Any authenticated HR user may review
     * a notification - review state is shared, not private to the creator.
     */
    public NotificationDto markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(NotificationNotFoundException::new);
        notification.markRead();
        notification = notificationRepository.save(notification);
        return toDto(notification);
    }

    /**
     * Deletes a notification. Delete is not restricted to the notification's
     * creator - notifications are a shared HR resource, so any authenticated
     * HR user may remove one.
     */
    public void deleteNotification(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(NotificationNotFoundException::new);
        notificationRepository.delete(notification);
    }

    private NotificationDto toDto(Notification notification) {
        String senderName = StringUtils.hasText(notification.getHrUserName())
                ? notification.getHrUserName()
                : notification.getHrUserEmail();

        return new NotificationDto(
                notification.getId(),
                notification.getEmployeeId(),
                notification.getEmployeeName(),
                notification.getDepartment(),
                notification.getComment(),
                notification.getCreatedAt(),
                notification.getHrUserEmail(),
                senderName,
                notification.isRead());
    }
}
