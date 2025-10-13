package com.creditcardservice.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign interceptor to propagate the incoming Authorization header
 * to downstream service calls (e.g., otp-service, transaction-service).
 * Fixes 401 errors from internal Feign calls that require bearer tokens.
 */
@Configuration
public class FeignAuthConfig {

    @Bean
    public RequestInterceptor authForwardingInterceptor() {
        return template -> {
            try {
                ServletRequestAttributes attrs =
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    String auth = attrs.getRequest().getHeader("Authorization");
                    if (auth != null && !auth.isBlank()) {
                        template.header("Authorization", auth);
                    }
                }
            } catch (Exception ignored) {
                // If no request context (e.g., async), skip forwarding
            }
        };
    }
}
