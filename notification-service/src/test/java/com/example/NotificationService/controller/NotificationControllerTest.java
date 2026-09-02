package com.example.NotificationService.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.NotificationService.dto.NotificationDto;
import com.example.NotificationService.exception.NotificationNotFoundException;
import com.example.NotificationService.security.JwtService;
import com.example.NotificationService.service.NotificationService;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtService jwtService;

    private static NotificationDto sampleNotification() {
        return new NotificationDto(1L, "5a94", "Leonelle Simco", "Sales",
                "Flight risk, discuss retention", Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void getMyNotificationsReturnsCurrentUsersNotifications() throws Exception {
        given(jwtService.extractEmail("valid-token")).willReturn("hr@example.com");
        given(notificationService.getNotificationsForUser("hr@example.com")).willReturn(List.of(sampleNotification()));

        mockMvc.perform(get("/notifications").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeName").value("Leonelle Simco"))
                .andExpect(jsonPath("$[0].comment").value("Flight risk, discuss retention"));
    }

    @Test
    void getMyNotificationsReturnsEmptyListWhenUserHasNone() throws Exception {
        given(jwtService.extractEmail("valid-token")).willReturn("hr@example.com");
        given(notificationService.getNotificationsForUser("hr@example.com")).willReturn(List.of());

        mockMvc.perform(get("/notifications").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getMyNotificationsWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteNotificationRemovesItAndReturns204() throws Exception {
        given(jwtService.extractEmail("valid-token")).willReturn("hr@example.com");

        mockMvc.perform(delete("/notifications/1").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteNotificationReturns404WhenNotFoundOrNotOwned() throws Exception {
        given(jwtService.extractEmail("valid-token")).willReturn("hr@example.com");
        doThrow(new NotificationNotFoundException())
                .when(notificationService).deleteNotification(99L, "hr@example.com");

        mockMvc.perform(delete("/notifications/99").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound());
    }
}
