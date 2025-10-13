package com.otp.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Forwards the incoming Authorization bearer token to downstream Feign calls.
 * If no incoming user token is present (e.g., public endpoints), it falls back
 * to a service-to-service token using Keycloak client credentials.
 */
@Configuration
public class FeignClientConfiguration {

    @Value("${keycloak.service-client.url}")
    private String keycloakUrl;

    @Value("${keycloak.service-client.realm}")
    private String keycloakRealm;

    @Value("${keycloak.service-client.client-id}")
    private String clientId;

    @Value("${keycloak.service-client.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private volatile String cachedToken;
    private volatile long tokenExpiryEpochSeconds;

    private String obtainServiceToken() {
        long now = Instant.now().getEpochSecond();
        if (cachedToken != null && now < tokenExpiryEpochSeconds - 30) {
            return cachedToken;
        }
        synchronized (this) {
            now = Instant.now().getEpochSecond();
            if (cachedToken != null && now < tokenExpiryEpochSeconds - 30) {
                return cachedToken;
            }
            String tokenUrl = keycloakUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body =
                    "grant_type=client_credentials" +
                    "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                    "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8);

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> resp = restTemplate.postForEntity(tokenUrl, entity, Map.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                Object at = resp.getBody().get("access_token");
                Object exp = resp.getBody().get("expires_in");
                if (at != null) {
                    cachedToken = at.toString();
                    long ttl = 300;
                    if (exp != null) {
                        try {
                            ttl = Long.parseLong(exp.toString());
                        } catch (Exception ignored) {}
                    }
                    tokenExpiryEpochSeconds = now + ttl;
                    System.out.println("OTP Service: Obtained service-to-service token from Keycloak.");
                    return cachedToken;
                }
            }
            throw new IllegalStateException("Failed to obtain service token: " + resp.getStatusCode());
        }
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            String authHeader = null;

            // Try to forward inbound Authorization header (from user requests)
            try {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                authHeader = Optional.ofNullable(attributes)
                        .map(ServletRequestAttributes::getRequest)
                        .map(request -> request.getHeader("Authorization"))
                        .orElse(null);
            } catch (Exception ignored) {}

            // Fallback: extract from Spring Security context (JWT auth)
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                try {
                    var authentication = SecurityContextHolder.getContext().getAuthentication();
                    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                        authHeader = "Bearer " + jwtAuth.getToken().getTokenValue();
                    }
                } catch (Exception ignored) {}
            }

            // Final fallback: service-to-service token (client credentials)
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                try {
                    String token = obtainServiceToken();
                    authHeader = "Bearer " + token;
                } catch (Exception ex) {
                    System.out.println("OTP Service: Failed to obtain service token for Feign call: " + ex.getMessage());
                }
            }

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                requestTemplate.header("Authorization", authHeader);
                System.out.println("Forwarding Authorization header from OTP Service Feign: "
                        + authHeader.substring(0, Math.min(authHeader.length(), 30)) + "...");
            } else {
                System.out.println("Authorization header not found for Feign call - proceeding without token");
            }
        };
    }
}
