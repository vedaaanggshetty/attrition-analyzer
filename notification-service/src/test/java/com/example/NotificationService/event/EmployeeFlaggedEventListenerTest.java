package com.example.NotificationService.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.NotificationService.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class EmployeeFlaggedEventListenerTest {

    @Mock
    private NotificationService notificationService;

    private EmployeeFlaggedEventListener listener;

    private static EmployeeFlaggedEvent sampleEvent() {
        return new EmployeeFlaggedEvent(
                UUID.randomUUID(), "5a94", "Leonelle Simco", "Sales", "Flight risk", "hr@example.com", Instant.now());
    }

    @Test
    void onEmployeeFlaggedCreatesNotification() {
        listener = new EmployeeFlaggedEventListener(notificationService);
        EmployeeFlaggedEvent event = sampleEvent();
        given(notificationService.createFromEvent(event)).willReturn(true);

        listener.onEmployeeFlagged(event);
    }

    @Test
    void onEmployeeFlaggedSwallowsOnlyTheEventIdDuplicateConstraintViolation() {
        listener = new EmployeeFlaggedEventListener(notificationService);
        EmployeeFlaggedEvent event = sampleEvent();
        willThrow(new DataIntegrityViolationException("Duplicate entry for key 'event_id'",
                new RuntimeException("Duplicate entry for key 'event_id'")))
                .given(notificationService).createFromEvent(event);

        listener.onEmployeeFlagged(event);
    }

    @Test
    void onEmployeeFlaggedPropagatesUnrelatedConstraintViolations() {
        listener = new EmployeeFlaggedEventListener(notificationService);
        EmployeeFlaggedEvent event = sampleEvent();
        willThrow(new DataIntegrityViolationException("Field 'hr_user_id' doesn't have a default value",
                new RuntimeException("Field 'hr_user_id' doesn't have a default value")))
                .given(notificationService).createFromEvent(event);

        assertThatThrownBy(() -> listener.onEmployeeFlagged(event))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void onEmployeeFlaggedPropagatesOtherFailures() {
        listener = new EmployeeFlaggedEventListener(notificationService);
        EmployeeFlaggedEvent event = sampleEvent();
        willThrow(new RuntimeException("db down"))
                .given(notificationService).createFromEvent(event);

        assertThatThrownBy(() -> listener.onEmployeeFlagged(event))
                .isInstanceOf(RuntimeException.class);
    }
}
