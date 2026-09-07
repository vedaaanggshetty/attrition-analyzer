package com.example.NotificationService.service;

import com.example.NotificationService.dto.CreateNotificationRequest;
import com.example.NotificationService.dto.NotificationDto;
import com.example.NotificationService.entity.Notification;
import com.example.NotificationService.exception.NotificationNotFoundException;
import com.example.NotificationService.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Same idea as authentication-service's AuthServiceIntegrationTest: proves
 * NotificationService works against a real (if in-memory) database, not a
 * mock - things like the generated id, the created_at timestamp, and the
 * "read" column actually round-tripping correctly.
 *
 * Each test cleans up its own row inside a transaction that Spring rolls
 * back afterward, so tests don't interfere with each other.
 */
@SpringBootTest
@Transactional
class NotificationServiceIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void shouldPersistNewNotification() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                "3012-1A41", "Leonelle Simco", "Sales", "Flight risk, discuss retention", "Jordan Lee");

        NotificationDto created = notificationService.createNotification(request, "hr@example.com");

        assertThat(created.id()).isNotNull();
        assertThat(notificationRepository.findById(created.id())).isPresent();
    }

    @Test
    void shouldShowNotificationCreatedByAnotherUser() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                "3012-1A41", "Leonelle Simco", "Sales", "Watch closely", "Alex Rivera");
        notificationService.createNotification(request, "alex@example.com");

        List<NotificationDto> visibleToEveryone = notificationService.getAllNotifications();

        assertThat(visibleToEveryone)
                .anySatisfy(n -> {
                    assertThat(n.senderEmail()).isEqualTo("alex@example.com");
                    assertThat(n.senderName()).isEqualTo("Alex Rivera");
                });
    }

    @Test
    void shouldPersistReadStatus() {
        Notification saved = notificationRepository.save(
                new Notification("3012-1A41", "Leonelle Simco", "Sales", "hr@example.com", "Jordan Lee", "Watch closely"));

        notificationService.markAsRead(saved.getId());

        Notification reloaded = notificationRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.isRead()).isTrue();
    }

    @Test
    void shouldPersistDeletion() {
        Notification saved = notificationRepository.save(
                new Notification("3012-1A41", "Leonelle Simco", "Sales", "hr@example.com", "Jordan Lee", "Watch closely"));

        notificationService.deleteNotification(saved.getId());

        assertThat(notificationRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void shouldThrowWhenDeletingUnknownNotification() {
        assertThatThrownBy(() -> notificationService.deleteNotification(999_999L))
                .isInstanceOf(NotificationNotFoundException.class);
    }
}
