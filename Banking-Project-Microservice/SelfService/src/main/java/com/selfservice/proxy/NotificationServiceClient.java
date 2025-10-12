package com.selfservice.proxy;

import com.selfservice.config.FeignAuthConfig;
import com.selfservice.dto.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for Notification Service.
 * Maps to NotificationService POST /notifications/send-email
 */
@FeignClient(name = "notification-service", path = "/notifications", configuration = FeignAuthConfig.class)
public interface NotificationServiceClient {

    @PostMapping("/send-email")
    void sendEmailNotification(@RequestBody NotificationRequest request);
}
