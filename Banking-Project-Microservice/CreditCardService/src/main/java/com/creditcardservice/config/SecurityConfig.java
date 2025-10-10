package com.creditcardservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Security configuration for CreditCardService to support method security with Keycloak roles.
 * Ensures hasRole('ADMIN') works by mapping JWT realm/resource roles to ROLE_* authorities.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                // expose actuator/health if needed later:
                //.requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler(new org.springframework.security.web.access.AccessDeniedHandlerImpl())
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();

            // Map Keycloak realm roles -> ROLE_*
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null) {
                Object rolesObj = realmAccess.get("roles");
                if (rolesObj instanceof Collection<?> roles) {
                    for (Object r : roles) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + r.toString().toUpperCase()));
                    }
                }
            }

            // Map Keycloak client roles (resource_access.*.roles) -> ROLE_*
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess != null) {
                for (Object clientObj : resourceAccess.values()) {
                    if (clientObj instanceof Map<?, ?> client) {
                        Object rolesObj = client.get("roles");
                        if (rolesObj instanceof Collection<?> roles) {
                            for (Object r : roles) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_" + r.toString().toUpperCase()));
                            }
                        }
                    }
                }
            }

            // Include scopes as SCOPE_*
            JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
            scopes.setAuthorityPrefix("SCOPE_");
            authorities.addAll(scopes.convert(jwt));

            return authorities;
        });

        return converter;
    }
}
