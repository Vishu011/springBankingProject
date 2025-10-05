package com.omnibank.cardmanagement.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Minimal bearer token auth for secure profile without external IdP.
 * Accepts Authorization: Bearer <token> where token is configured via:
 * - property: card-management.security.adminBearerToken
 * - or env: ADMIN_BEARER_TOKEN
 */
@Component
@Profile("secure")
public class TokenAuthFilter extends OncePerRequestFilter {

  private final String expected;

  public TokenAuthFilter(
      @Value("${card-management.security.adminBearerToken:${ADMIN_BEARER_TOKEN:admin-token-dev}}") String expected) {
    this.expected = expected;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain
  ) throws ServletException, IOException {
    String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (auth != null && auth.startsWith("Bearer ")) {
      String token = auth.substring("Bearer ".length()).trim();
      if (!token.isBlank() && token.equals(expected)) {
        AbstractAuthenticationToken authentication =
            new AbstractAuthenticationToken(AuthorityUtils.createAuthorityList("ROLE_ADMIN")) {
              @Override public Object getCredentials() { return token; }
              @Override public Object getPrincipal() { return "admin"; }
              @Override public boolean isAuthenticated() { return true; }
            };
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    }
    filterChain.doFilter(request, response);
  }
}
