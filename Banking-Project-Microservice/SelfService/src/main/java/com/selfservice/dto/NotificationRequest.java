package com.selfservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO mirroring NotificationService's request.
 * Fields: userId, type (e.g., "EMAIL"), content, toEmail (optional for public flows).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private String userId;
    private String type;    // "EMAIL" | "SMS"
    private String content;
    // Optional direct recipient email for special flows
    private String toEmail;
}
