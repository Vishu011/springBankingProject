package com.selfservice.proxy;

import com.selfservice.config.FeignAuthConfig;
import com.selfservice.proxy.dto.UserResponse;
import com.selfservice.proxy.dto.UserUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for User Service.
 * Uses Authorization header forwarding via FeignAuthConfig.
 */
@FeignClient(name = "user-service", path = "/auth", configuration = FeignAuthConfig.class)
public interface UserServiceClient {

    @GetMapping("/user/{userId}")
    UserResponse getUserById(@PathVariable("userId") String userId);

    @PutMapping("/user/{userId}")
    UserResponse updateUser(@PathVariable("userId") String userId, @RequestBody UserUpdateRequest request);
}
