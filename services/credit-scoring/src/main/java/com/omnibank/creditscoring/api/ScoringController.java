package com.omnibank.creditscoring.api;

import com.omnibank.creditscoring.application.ScoringService;
import com.omnibank.creditscoring.application.ScoringService.BureauData;
import com.omnibank.creditscoring.application.ScoringService.ScoringResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Dev-local mock credit scoring endpoint (blocking).
 * POST /api/v1/internal/scoring/calculate
 */
@RestController
@RequestMapping("/api/v1/internal/scoring")
@RequiredArgsConstructor
public class ScoringController {

  public static final String HDR_CORRELATION_ID = "X-Correlation-Id";

  private final ScoringService scoringService;

  @PostMapping("/calculate")
  public ResponseEntity<ScoringResponse> calculate(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @Valid @RequestBody ScoringRequest request
  ) {
    String cid = ensureCorrelationId(correlationId);
    ScoringResult res = scoringService.calculate(request.getApplicantId());
    ScoringResponse body = new ScoringResponse(
        res.creditScore(),
        res.probabilityOfDefault(),
        res.keyFactors(),
        res.bureauData()
    );
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .body(body);
  }

  private static String ensureCorrelationId(String v) {
    return StringUtils.hasText(v) ? v : java.util.UUID.randomUUID().toString();
  }

  @Data
  public static class ScoringRequest {
    @NotBlank
    private String applicantId;
  }

  public record ScoringResponse(
      int creditScore,
      BigDecimal probabilityOfDefault,
      List<String> keyFactors,
      BureauData bureauData
  ) {}
}
