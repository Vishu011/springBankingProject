package com.omnibank.cardmanagement.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Minimal security configuration for secure profile.
 * - GET endpoints are permitted
 * - POST/PUT/PATCH/DELETE require authentication (admin token via TokenAuthFilter)
 * - CSRF disabled for simplicity of API usage
 */
@Configuration
@Profile("secure")
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, TokenAuthFilter tokenAuthFilter) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            // allow actuator health/info without auth
            .requestMatchers("/actuator/health", "/actuator/info").permitAll()
            // Allow GET reads
            .requestMatchers(org.springframework.http.HttpMethod.GET, "/**").permitAll()
            // Everything else requires authentication
            .anyRequest().authenticated()
        )
        .addFilterBefore(tokenAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .httpBasic(Customizer.withDefaults())
        .formLogin(form -> form.disable());

    return http.build();
  }
}
