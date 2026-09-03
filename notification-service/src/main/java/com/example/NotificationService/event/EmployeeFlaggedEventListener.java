package com.example.NotificationService.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.NotificationService.service.NotificationService;

/**
 * Consumes employee.flagged and turns each event into a Notification.
 * The eventId unique constraint is the authoritative idempotency guard - the
 * existsByEventId() pre-check in NotificationService is just the fast path;
 * a concurrent duplicate delivery that races past it still fails safely on
 * the DB constraint here. Any other constraint violation (e.g. a genuine
 * schema/data problem) is NOT a duplicate and must not be swallowed - it's
 * rethrown so the container's error handler retries/logs it as a failure.
 */
@Component
public class EmployeeFlaggedEventListener {

    private static final Logger log = LoggerFactory.getLogger(EmployeeFlaggedEventListener.class);

    private final NotificationService notificationService;

    public EmployeeFlaggedEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "${notification.kafka.topic}", groupId = "notification-service")
    public void onEmployeeFlagged(EmployeeFlaggedEvent event) {
        try {
            boolean created = notificationService.createFromEvent(event);
            if (!created) {
                log.info("Ignored duplicate EmployeeFlaggedEvent eventId={}", event.eventId());
            }
        } catch (DataIntegrityViolationException ex) {
            if (isEventIdUniqueConstraintViolation(ex)) {
                log.info("Ignored duplicate EmployeeFlaggedEvent eventId={} (constraint)", event.eventId());
                return;
            }
            throw ex;
        }
    }

    private boolean isEventIdUniqueConstraintViolation(DataIntegrityViolationException ex) {
        String message = String.valueOf(ex.getMostSpecificCause().getMessage()).toLowerCase();
        return message.contains("event_id");
    }
}
