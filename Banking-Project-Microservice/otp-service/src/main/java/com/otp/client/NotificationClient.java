package com.otp.client;

import com.otp.client.dto.NotificationRequest;
import org.springframework.http.ResponseEntity;
import com.otp.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", path = "/notifications", configuration = FeignClientConfiguration.class)
public interface NotificationClient {

    @PostMapping("/send-email")
    ResponseEntity<Void> sendEmail(@RequestBody NotificationRequest request);
}
