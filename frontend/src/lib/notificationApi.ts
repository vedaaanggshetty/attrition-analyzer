import { apiRequest } from "./apiClient";

// Mirrors notification-service's NotificationDto (see NotificationController).
// HR-only (US-21) - every call here requires a JWT; Guests never reach these.

export interface Notification {
  id: number;
  employeeId: string;
  employeeName: string;
  department: string;
  comment: string;
  createdAt: string;
}

export function getMyNotifications(): Promise<Notification[]> {
  return apiRequest<Notification[]>("/notifications");
}

export function createNotification(request: {
  employeeId: string;
  employeeName: string;
  department: string;
  comment: string;
}): Promise<Notification> {
  return apiRequest<Notification>("/notifications", { method: "POST", body: request });
}

export function deleteNotification(id: number): Promise<void> {
  return apiRequest<void>(`/notifications/${id}`, { method: "DELETE" });
}
