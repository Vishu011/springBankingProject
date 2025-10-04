package com.omnibank.loanmanagement.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Standardized error body for HTTP errors:
 * { timestamp, status, error, message, correlationId, path }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  public static final String HDR_CORRELATION_ID = "X-Correlation-Id";

  @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class, MethodArgumentNotValidException.class })
  public ResponseEntity<Map<String, Object>> handleBadRequest(
      Exception ex,
      HttpServletRequest request,
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId
  ) {
    String cid = ensureCorrelationId(correlationId);
    String message = ex instanceof MethodArgumentNotValidException manve
        ? (manve.getBindingResult().getFieldError() != null
            ? manve.getBindingResult().getFieldError().getDefaultMessage()
            : "Validation failed")
        : ex.getMessage();

    Map<String, Object> body = baseBody(HttpStatus.BAD_REQUEST, message, cid, request.getRequestURI());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .header(HDR_CORRELATION_ID, cid)
        .body(body);
  }

  @ExceptionHandler(Throwable.class)
  public ResponseEntity<Map<String, Object>> handleServerError(
      Throwable ex,
      HttpServletRequest request,
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId
  ) {
    String cid = ensureCorrelationId(correlationId);
    Map<String, Object> body = baseBody(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", cid, request.getRequestURI());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .header(HDR_CORRELATION_ID, cid)
        .body(body);
  }

  private static Map<String, Object> baseBody(HttpStatus status, String message, String correlationId, String path) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", Instant.now().toString());
    body.put("status", status.value());
    body.put("error", status.getReasonPhrase());
    body.put("message", message);
    body.put("correlationId", correlationId);
    body.put("path", path);
    return body;
  }

  private static String ensureCorrelationId(String value) {
    return StringUtils.hasText(value) ? value : UUID.randomUUID().toString();
  }
}
