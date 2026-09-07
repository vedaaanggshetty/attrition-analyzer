package com.example.NotificationService.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.NotificationService.client.UserProfileClient;
import com.example.NotificationService.dto.CreateNotificationRequest;
import com.example.NotificationService.dto.InternalProfileResponse;
import com.example.NotificationService.dto.NotificationDto;
import com.example.NotificationService.entity.Notification;
import com.example.NotificationService.event.EmployeeFlaggedEvent;
import com.example.NotificationService.exception.NotificationNotFoundException;
import com.example.NotificationService.repository.NotificationRepository;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    // Matches a bare UUID such as "650f54e6-f7df-4c9c-a3a2-350369d2d830". A fixed bug
    // (JwtService.extractEmail returning the JWT subject/userId instead of the email
    // claim) means some pre-fix notifications have this stored in hr_user_email instead
    // of a real email address - never display it as if it were a name/email.
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final NotificationRepository notificationRepository;
    private final UserProfileClient userProfileClient;

    public NotificationService(NotificationRepository notificationRepository, UserProfileClient userProfileClient) {
        this.notificationRepository = notificationRepository;
        this.userProfileClient = userProfileClient;
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
        String senderName = resolveSenderName(notification);

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

    // hrUserName is the preferred display name. If it's missing, fall back to the
    // email - unless that email is actually a bare UUID (a pre-fix legacy row, see
    // UUID_PATTERN above), in which case it's really a userId: resolve it against
    // User Profile Service (the same store Authentication/frontend already treat as
    // the source of truth for a user's display name) rather than showing the UUID
    // or a fabricated name. Only falls back to a generic label if that lookup
    // genuinely can't resolve a name (profile deleted, service unavailable, etc).
    private String resolveSenderName(Notification notification) {
        if (StringUtils.hasText(notification.getHrUserName())) {
            return notification.getHrUserName();
        }
        String email = notification.getHrUserEmail();
        if (!StringUtils.hasText(email)) {
            return "Unknown HR user";
        }
        if (!UUID_PATTERN.matcher(email).matches()) {
            return email;
        }
        return resolveNameByUserId(email);
    }

    private String resolveNameByUserId(String userId) {
        try {
            InternalProfileResponse profile = userProfileClient.getProfile(userId);
            if (profile != null && StringUtils.hasText(profile.fullName())) {
                return profile.fullName();
            }
        } catch (RuntimeException ex) {
            // Profile not found (404), User Profile Service unavailable, or unresolvable
            // via service discovery - none of these should break notification listing,
            // so fall through to the generic label below.
            log.warn("Could not resolve HR display name for legacy userId {}: {}", userId, ex.getMessage());
        }
        return "Unknown HR user";
    }
}
