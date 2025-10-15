package com.bank.aiorchestrator.integrations.user;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.bank.aiorchestrator.config.FeignOAuth2Config;
import com.bank.aiorchestrator.integrations.user.dto.UserProfileResponse;

/**
 * Feign client to fetch user profile details from UserMicroservice.
 */
@FeignClient(
        name = "user-profile-client",
        url = "${orchestrator.integrations.gatewayBaseUrl}",
        configuration = FeignOAuth2Config.class
)
public interface UserProfileClient {

    @GetMapping(value = "/auth/user/{userId}", consumes = MediaType.ALL_VALUE)
    UserProfileResponse getUserById(@PathVariable("userId") String userId);
}
