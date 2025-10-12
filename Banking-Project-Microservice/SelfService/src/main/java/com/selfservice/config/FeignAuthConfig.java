package com.selfservice.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign interceptor to forward Authorization header to downstream services.
 * Ensures SelfService calls to User/OTP/Notification services carry the caller's JWT
 * for proper authorization checks.
 */
@Configuration
public class FeignAuthConfig {

    @Bean
    public RequestInterceptor authForwardingInterceptor() {
        return template -> {
            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    String auth = attrs.getRequest().getHeader("Authorization");
                    if (auth != null && !auth.isBlank()) {
                        template.header("Authorization", auth);
                    }
                }
            } catch (Exception ignored) {
                // No web request context (e.g., async threads). Skip header forward.
            }
        };
    }
}
