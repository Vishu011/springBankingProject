package com.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for sending notifications via NotificationService.
 * - userId: the user to notify (used to resolve email if toEmail is not provided)
 * - type: "EMAIL" | "SMS" (currently only EMAIL is implemented)
 * - content: message body
 * - toEmail: optional explicit email; if null, service will look up user's email
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private String userId;
    private String type;
    private String content;
    private String toEmail;
}
