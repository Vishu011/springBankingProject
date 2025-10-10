package com.creditcardservice.proxyservice;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.creditcardservice.dto.NotificationRequestDto;

@FeignClient(name = "notification-service", path = "/notifications")
public interface NotificationServiceClient {

    @PostMapping("/send-email")
    void sendEmailNotification(@RequestBody NotificationRequestDto requestDto);

    @PostMapping("/send-sms")
    void sendSmsNotification(@RequestBody NotificationRequestDto requestDto);
}
