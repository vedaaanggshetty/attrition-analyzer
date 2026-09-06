import { apiRequest } from "./apiClient";

// Mirrors notification-service's NotificationDto (see NotificationController).
// HR-only (US-21) - every call here requires a JWT; Guests never reach these.
// Notifications are shared across every HR user - senderEmail identifies who
// created it, `read` is a shared review flag, not per-viewer state.

export interface Notification {
  id: number;
  employeeId: string;
  employeeName: string;
  department: string;
  comment: string;
  createdAt: string;
  senderEmail: string;
  // Display name of the sender - falls back to their email server-side when
  // no name is known (see notification-service's NotificationService.toDto).
  senderName: string;
  read: boolean;
}

export function getAllNotifications(): Promise<Notification[]> {
  return apiRequest<Notification[]>("/notifications");
}

export function createNotification(request: {
  employeeId: string;
  employeeName: string;
  department: string;
  comment: string;
  hrUserName?: string;
}): Promise<Notification> {
  return apiRequest<Notification>("/notifications", { method: "POST", body: request });
}

export function markNotificationRead(id: number): Promise<Notification> {
  return apiRequest<Notification>(`/notifications/${id}/read`, { method: "PATCH" });
}

export function deleteNotification(id: number): Promise<void> {
  return apiRequest<void>(`/notifications/${id}`, { method: "DELETE" });
}
