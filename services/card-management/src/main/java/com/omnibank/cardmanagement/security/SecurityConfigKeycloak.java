package com.omnibank.cardmanagement.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Keycloak-backed Resource Server security config.
 * Active under profile 'secure-keycloak'. Expects JWTs issued by Keycloak (see application-secure-keycloak.yml).
 */
@Configuration
@Profile("secure-keycloak")
public class SecurityConfigKeycloak {

  @Bean
  public SecurityFilterChain keycloakSecurity(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health", "/actuator/info").permitAll()
            .requestMatchers(HttpMethod.GET, "/**").permitAll()
            // Require admin scope/role for mutating operations
            .requestMatchers(HttpMethod.POST, "/**").hasAnyAuthority("SCOPE_admin", "ROLE_admin")
            .requestMatchers(HttpMethod.PUT, "/**").hasAnyAuthority("SCOPE_admin", "ROLE_admin")
            .requestMatchers(HttpMethod.PATCH, "/**").hasAnyAuthority("SCOPE_admin", "ROLE_admin")
            .requestMatchers(HttpMethod.DELETE, "/**").hasAnyAuthority("SCOPE_admin", "ROLE_admin")
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
    return http.build();
  }
}
