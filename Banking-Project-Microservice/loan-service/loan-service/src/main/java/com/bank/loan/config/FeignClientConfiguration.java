package com.bank.loan.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Configuration
public class FeignClientConfiguration {

    /**
     * Creates a RequestInterceptor that adds the Authorization header to outgoing Feign requests.
     * This ensures that the JWT from the incoming request is forwarded to downstream microservices.
     * This bean is specifically for Feign client configuration.
     *
     * @return A RequestInterceptor bean.
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            String authHeader = null;

            // Try to extract token from current HTTP request (Servlet context)
            try {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                authHeader = Optional.ofNullable(attributes)
                        .map(ServletRequestAttributes::getRequest)
                        .map(request -> request.getHeader("Authorization"))
                        .orElse(null);
            } catch (Exception ignored) {}

            // Fallback: try to extract token from Spring Security context (works even when executed on a different thread)
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                try {
                    var authentication = SecurityContextHolder.getContext().getAuthentication();
                    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                        authHeader = "Bearer " + jwtAuth.getToken().getTokenValue();
                    }
                } catch (Exception ignored) {}
            }

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                requestTemplate.header("Authorization", authHeader);
                System.out.println("Forwarding Authorization header from Loan Service Feign: "
                        + authHeader.substring(0, Math.min(authHeader.length(), 30)) + "...");
            } else {
                System.out.println("Authorization header not found for Feign call - proceeding without token");
            }
        };
    }
}
