package com.omnibank.loanorigination.api;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Consistent JSON error responses with correlationId.
 * Aligns behavior with other services: 400 for validation/illegal args, 409 for illegal state.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, Object> body = base();
    body.put("status", 400);
    body.put("error", "Bad Request");
    body.put("message", ex.getBindingResult().getAllErrors().stream()
        .findFirst().map(err -> err.getDefaultMessage()).orElse("Validation failed"));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
    Map<String, Object> body = base();
    body.put("status", 400);
    body.put("error", "Bad Request");
    body.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
    Map<String, Object> body = base();
    body.put("status", 409);
    body.put("error", "Conflict");
    body.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
    Map<String, Object> body = base();
    body.put("status", 500);
    body.put("error", "Internal Server Error");
    body.put("message", "Unexpected error");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }

  private static Map<String, Object> base() {
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", Instant.now().toString());
    body.put("correlationId", MDC.get("correlationId"));
    return body;
  }
}
