package com.example.NotificationService.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.NotificationService.dto.CreateNotificationRequest;
import com.example.NotificationService.dto.NotificationDto;
import com.example.NotificationService.entity.Notification;
import com.example.NotificationService.event.EmployeeFlaggedEvent;
import com.example.NotificationService.exception.NotificationNotFoundException;
import com.example.NotificationService.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository);
    }

    private static Notification sampleNotification(String owner) {
        return new Notification("5a94", "Leonelle Simco", "Sales", owner, "HR User", "Flight risk, discuss retention");
    }

    @Test
    void createNotificationSavesAndReturnsNotificationForCurrentUser() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                "5a94", "Leonelle Simco", "Sales", "Flight risk, discuss retention", "HR User");
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        given(notificationRepository.save(captor.capture())).willAnswer(invocation -> invocation.getArgument(0));

        NotificationDto result = notificationService.createNotification(request, "hr@example.com");

        Notification saved = captor.getValue();
        assertThat(saved.getEmployeeId()).isEqualTo("5a94");
        assertThat(saved.getEmployeeName()).isEqualTo("Leonelle Simco");
        assertThat(saved.getDepartment()).isEqualTo("Sales");
        assertThat(saved.getHrUserEmail()).isEqualTo("hr@example.com");
        assertThat(saved.getHrUserName()).isEqualTo("HR User");
        assertThat(saved.getComment()).isEqualTo("Flight risk, discuss retention");

        assertThat(result.employeeName()).isEqualTo("Leonelle Simco");
        assertThat(result.comment()).isEqualTo("Flight risk, discuss retention");
        assertThat(result.senderName()).isEqualTo("HR User");
    }

    @Test
    void createFromEventSavesNotificationWithEventId() {
        UUID eventId = UUID.randomUUID();
        EmployeeFlaggedEvent event = new EmployeeFlaggedEvent(
                eventId, "5a94", "Leonelle Simco", "Sales", "Flight risk", "hr@example.com", "HR User", Instant.now());
        given(notificationRepository.existsByEventId(eventId)).willReturn(false);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        given(notificationRepository.save(captor.capture())).willAnswer(invocation -> invocation.getArgument(0));

        boolean created = notificationService.createFromEvent(event);

        assertThat(created).isTrue();
        Notification saved = captor.getValue();
        assertThat(saved.getEventId()).isEqualTo(eventId);
        assertThat(saved.getEmployeeName()).isEqualTo("Leonelle Simco");
        assertThat(saved.getHrUserEmail()).isEqualTo("hr@example.com");
        assertThat(saved.getHrUserName()).isEqualTo("HR User");
    }

    @Test
    void createFromEventIsNoOpForDuplicateEventId() {
        UUID eventId = UUID.randomUUID();
        EmployeeFlaggedEvent event = new EmployeeFlaggedEvent(
                eventId, "5a94", "Leonelle Simco", "Sales", "Flight risk", "hr@example.com", "HR User", Instant.now());
        given(notificationRepository.existsByEventId(eventId)).willReturn(true);

        boolean created = notificationService.createFromEvent(event);

        assertThat(created).isFalse();
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void getAllNotificationsReturnsMappedListRegardlessOfCreator() {
        given(notificationRepository.findAllByOrderByCreatedAtDesc())
                .willReturn(List.of(sampleNotification("someone-else@example.com")));

        List<NotificationDto> result = notificationService.getAllNotifications();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).employeeName()).isEqualTo("Leonelle Simco");
        assertThat(result.get(0).comment()).isEqualTo("Flight risk, discuss retention");
        assertThat(result.get(0).senderEmail()).isEqualTo("someone-else@example.com");
    }

    @Test
    void getAllNotificationsFallsBackToEmailWhenNameMissing() {
        Notification notification = new Notification(
                "5a94", "Leonelle Simco", "Sales", "someone-else@example.com", null, "Flight risk, discuss retention");
        given(notificationRepository.findAllByOrderByCreatedAtDesc()).willReturn(List.of(notification));

        List<NotificationDto> result = notificationService.getAllNotifications();

        assertThat(result.get(0).senderName()).isEqualTo("someone-else@example.com");
    }

    @Test
    void getAllNotificationsReturnsEmptyListWhenNoneExist() {
        given(notificationRepository.findAllByOrderByCreatedAtDesc()).willReturn(List.of());

        assertThat(notificationService.getAllNotifications()).isEmpty();
    }

    @Test
    void markAsReadSetsReadFlag() {
        Notification notification = sampleNotification("hr@example.com");
        given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));
        given(notificationRepository.save(notification)).willReturn(notification);

        NotificationDto result = notificationService.markAsRead(1L);

        assertThat(notification.isRead()).isTrue();
        assertThat(result.read()).isTrue();
    }

    @Test
    void markAsReadThrowsWhenNotFound() {
        given(notificationRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(99L))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    @Test
    void deleteNotificationRemovesNotificationRegardlessOfCreator() {
        Notification notification = sampleNotification("someone-else@example.com");
        given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

        notificationService.deleteNotification(1L);

        verify(notificationRepository).delete(notification);
    }

    @Test
    void deleteNotificationThrowsWhenNotFound() {
        given(notificationRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.deleteNotification(99L))
                .isInstanceOf(NotificationNotFoundException.class);
    }
}
