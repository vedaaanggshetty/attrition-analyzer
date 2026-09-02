package com.example.NotificationService.exception;

/**
 * Thrown when a notification doesn't exist, or exists but belongs to a
 * different HR user. Both cases return the same generic message so a
 * response never reveals whether a given id belongs to someone else.
 */
public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException() {
        super("Notification not found");
    }
}
