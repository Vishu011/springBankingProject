package com.omnibank.paymentgateway.api;

import com.omnibank.paymentgateway.api.dto.InitiatePaymentResponse;
import com.omnibank.paymentgateway.api.dto.InternalTransferRequest;
import com.omnibank.paymentgateway.application.PaymentGatewayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for payment initiations and status checks.
 * - POST /api/v1/payments/internal-transfer
 * - GET  /api/v1/payments/{paymentId}/status
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

  public static final String HDR_CORRELATION_ID = "X-Correlation-Id";
  public static final String HDR_IDEMPOTENCY_KEY = "Idempotency-Key";

  private final PaymentGatewayService service;

  @PostMapping("/internal-transfer")
  public ResponseEntity<InitiatePaymentResponse> internalTransfer(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @RequestHeader(value = HDR_IDEMPOTENCY_KEY, required = false) String idempotencyKey,
      @Valid @RequestBody InternalTransferRequest request
  ) {
    String cid = ensureCorrelationId(correlationId);
    var result = service.initiateInternalTransfer(request, cid, idempotencyKey);
    return ResponseEntity.accepted()
        .header(HDR_CORRELATION_ID, cid)
        .body(new InitiatePaymentResponse(result.paymentId(), result.status()));
  }

  @GetMapping("/{paymentId}/status")
  public ResponseEntity<String> getStatus(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("paymentId") String paymentId
  ) {
    String cid = ensureCorrelationId(correlationId);
    String status = service.getStatusByUuid(paymentId);
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .body(status);
  }

  private String ensureCorrelationId(String headerValue) {
    return StringUtils.hasText(headerValue) ? headerValue : java.util.UUID.randomUUID().toString();
  }
}
