package com.accountMicroservice.proxyService;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.accountMicroservice.config.FeignClientConfiguration;
import com.accountMicroservice.dto.NotificationRequest;

/**
 * Feign client for Notification Service.
 * Maps to NotificationService POST /notifications/send-email
 */
@FeignClient(name = "notification-service", path = "/notifications", configuration = FeignClientConfiguration.class)
public interface NotificationServiceClient {

    @PostMapping("/send-email")
    void sendEmailNotification(@RequestBody NotificationRequest request);

}
