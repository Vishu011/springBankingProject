package com.omnibank.frauddetection.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Dev-only synchronous fraud scoring endpoint used by payment-gateway.
 * Path expected by gateway: POST /api/v1/internal/dev/score
 *
 * Heuristics (simple placeholder):
 * - amount >= 10000 -> BLOCK (score 0.90)
 * - amount >= 2000 and < 10000 -> CHALLENGE (score 0.50)
 * - otherwise -> ALLOW (score 0.05)
 */
@RestController
@RequestMapping("/api/v1/internal/dev")
public class FraudController {

  public static final String HDR_CORRELATION_ID = "X-Correlation-Id";

  @PostMapping("/score")
  public ResponseEntity<Decision> score(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @Valid @RequestBody FraudRequest request
  ) {
    String cid = ensureCorrelationId(correlationId);

    BigDecimal amt = request.getAmount();
    String action;
    BigDecimal score;

    if (amt.compareTo(new BigDecimal("10000.00")) >= 0) {
      action = "BLOCK";
      score = new BigDecimal("0.90");
    } else if (amt.compareTo(new BigDecimal("2000.00")) >= 0) {
      action = "CHALLENGE";
      score = new BigDecimal("0.50");
    } else {
      action = "ALLOW";
      score = new BigDecimal("0.05");
    }

    Decision body = new Decision(action, score);
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .body(body);
  }

  private static String ensureCorrelationId(String v) {
    return StringUtils.hasText(v) ? v : java.util.UUID.randomUUID().toString();
  }

  // Request DTO expected by payment-gateway client
  @Data
  public static class FraudRequest {
    @NotNull
    private Long customerId;

    @NotBlank
    private String fromAccount;

    @NotBlank
    private String toAccount;

    @NotNull
    @DecimalMin(value = "0.01", message = "amount must be > 0")
    private BigDecimal amount;

    @NotBlank
    private String currency;
  }

  // Response DTO consumed by payment-gateway client
  @Data
  public static class Decision {
    private String action;   // ALLOW | CHALLENGE | BLOCK
    private BigDecimal score;

    public Decision() {}
    public Decision(String action, BigDecimal score) {
      this.action = action;
      this.score = score;
    }
  }
}
