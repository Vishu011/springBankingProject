package com.otp.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "otp")
public class OtpProperties {
    private int codeLength = 6;
    private Map<String, Integer> ttlSeconds;
    private int maxAttempts = 5;

    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class RateLimit {
        private int windowSeconds = 60;
        private int maxRequestsPerWindow = 1;
    }

    public int ttlForPurpose(String purposeKey) {
        if (ttlSeconds == null) {
            return 300;
        }
        Integer v = ttlSeconds.get(purposeKey);
        if (v == null) {
            v = ttlSeconds.get("default");
        }
        return v != null ? v : 300;
    }
}
