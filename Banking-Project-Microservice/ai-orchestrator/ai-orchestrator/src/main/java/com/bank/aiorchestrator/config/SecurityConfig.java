package com.bank.aiorchestrator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Development security configuration:
 * - Allows Angular admin UI (http://localhost:4300) to call /agent/** endpoints without auth
 * - Permits CORS preflight (OPTIONS) requests
 * - Disables CSRF (for simple dev calls) and default login redirects
 *
 * NOTE: Tighten this for production (protect non-/agent endpoints behind gateway / OAuth2).
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Enable Spring Security CORS processing (CorsFilter bean already configured)
            .cors(Customizer.withDefaults())
            // Disable CSRF for dev (or scope this to /agent/** as needed)
            .csrf(csrf -> csrf.disable())
            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/agent/**", "/actuator/**").permitAll()
                .anyRequest().permitAll() // Relax for dev; lock down for prod as needed
            )
            // Avoid default login page / redirects
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
