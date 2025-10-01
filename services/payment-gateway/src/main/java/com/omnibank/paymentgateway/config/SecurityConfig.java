package com.omnibank.paymentgateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Temporary permissive security for dev-local to unblock core functionality.
 * TODO: Switch to OAuth2 Resource Server (Keycloak) when security stack is ready.
 */
@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/actuator/**",
                "/v3/api-docs/**",
                "/swagger-ui.html",
                "/swagger-ui/**"
            ).permitAll()
            .anyRequest().permitAll()
        )
        .httpBasic(Customizer.withDefaults());
    return http.build();
  }
}
