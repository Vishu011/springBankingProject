package com.accountMicroservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Configuration
@EnableWebSecurity // Enables Spring Security's web security support
@EnableMethodSecurity(prePostEnabled = true) // Enables method-level security annotations like @PreAuthorize
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless REST APIs. JWTs provide protection against CSRF.
            .csrf(csrf -> csrf.disable())
            // Configure authorization rules for HTTP requests
            .authorizeHttpRequests(authorize -> authorize
                // Allow H2 console for development (if exposed directly, which is not recommended for prod)
                .requestMatchers("/h2-console/**").permitAll()
                // All other requests must be authenticated.
                // This is the crucial line that ensures all other endpoints require a valid JWT.
                .anyRequest().authenticated()
            )
            // Configure OAuth2 Resource Server for JWT validation
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    // Use a custom JWT converter to extract roles from Keycloak JWT claims
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            // Ensure proper 401/403 responses instead of generic 500
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler(new org.springframework.security.web.access.AccessDeniedHandlerImpl())
            )
            // Configure session management to be stateless, as JWTs handle authentication per request
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );
        return http.build();
    }

    /**
     * Configures a custom JwtAuthenticationConverter to extract roles (authorities) from the JWT.
     * Keycloak typically puts roles in a custom claim (e.g., "realm_access.roles" for realm roles).
     * @return A configured JwtAuthenticationConverter.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();

        // Custom converter that extracts roles from:
        //  - realm_access.roles           (Keycloak realm roles)
        //  - resource_access.*.roles      (Keycloak client roles for any client)
        //  - scope/scope-like claims      (as SCOPE_* authorities)
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();

            // Realm roles
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null) {
                Object rolesObj = realmAccess.get("roles");
                if (rolesObj instanceof Collection<?> roles) {
                    for (Object roleObj : roles) {
                        String role = roleObj.toString();
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                    }
                }
            }

            // Client roles (iterate all clients present in resource_access)
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess != null) {
                for (Object clientObj : resourceAccess.values()) {
                    if (clientObj instanceof Map<?, ?> client) {
                        Object rolesObj = client.get("roles");
                        if (rolesObj instanceof Collection<?> roles) {
                            for (Object roleObj : roles) {
                                String role = roleObj.toString();
                                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                            }
                        }
                    }
                }
            }

            // Include scope authorities as SCOPE_*
            JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
            scopeConverter.setAuthorityPrefix("SCOPE_");
            authorities.addAll(scopeConverter.convert(jwt));

            return authorities;
        });

        return jwtAuthenticationConverter;
    }
}
