package com.otp.client.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class NotificationResponse {
    private String notificationId;
    private String userId;
    private String type;
    private String content;
    private String status;
    private LocalDateTime sentAt;
    private String message;
}
