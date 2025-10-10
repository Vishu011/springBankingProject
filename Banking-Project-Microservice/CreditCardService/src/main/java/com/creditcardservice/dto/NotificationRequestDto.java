package com.creditcardservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequestDto {
    private String userId;
    private NotificationType type;
    private String content;

    public enum NotificationType {
        EMAIL,
        SMS,
        IN_APP,
        PUSH
    }
}
