package com.omnibank.onboarding.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Ensures every request carries a correlation ID for tracing/log correlation.
 * - Reads X-Correlation-Id from the request header; generates a UUID if missing
 * - Puts it into MDC (key: "correlationId") so all logs include it
 * - Sets the same header on the response for client propagation
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

  public static final String HEADER = "X-Correlation-Id";
  public static final String MDC_KEY = "correlationId";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String cid = request.getHeader(HEADER);
    if (cid == null || cid.isBlank()) {
      cid = UUID.randomUUID().toString();
    }

    // Put into MDC for structured logging across the request lifecycle
    MDC.put(MDC_KEY, cid);

    // Ensure response also carries the correlation ID
    response.setHeader(HEADER, cid);

    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }
}
