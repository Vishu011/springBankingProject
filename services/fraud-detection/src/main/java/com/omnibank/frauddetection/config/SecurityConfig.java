package com.omnibank.frauddetection.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Permissive dev-local security to unblock integration.
 * TODO: replace with OAuth2 Resource Server (Keycloak) in hardening phase.
 */
@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/actuator/**",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/api/v1/internal/dev/**"
            ).permitAll()
            .anyRequest().permitAll()
        )
        .httpBasic(Customizer.withDefaults())
        .formLogin(form -> form.disable());
    return http.build();
  }
}
