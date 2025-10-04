package com.omnibank.cardmanagement.api;

import com.omnibank.cardmanagement.application.CardManagementService;
import com.omnibank.cardmanagement.application.CardManagementService.CardView;
import com.omnibank.cardmanagement.application.CardManagementService.Created;
import com.omnibank.cardmanagement.application.CardManagementService.CreateCardRequest;
import com.omnibank.cardmanagement.application.CardManagementService.UpdateLimitsRequest;
import com.omnibank.cardmanagement.application.CardManagementService.UpdateStatusRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Card Management APIs (dev-open)
 * - POST /api/v1/cards/dev/create
 * - GET  /api/v1/cards/{cardId}
 * - POST /api/v1/cards/{cardId}/activate
 * - POST /api/v1/cards/{cardId}/status (BLOCK|UNBLOCK)
 * - POST /api/v1/cards/{cardId}/limits
 */
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@Tag(name = "Card Management")
public class CardManagementController {

  public static final String HDR_CORRELATION_ID = "X-Correlation-Id";

  private final CardManagementService service;

  // Dev-only: create a card record directly
  @PostMapping("/dev/create")
  @Operation(summary = "Dev-only create card record")
  public ResponseEntity<Created> createDev(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @Valid @RequestBody CreateCardRequest request
  ) {
    String cid = ensureCorrelationId(correlationId);
    Created res = service.createDev(request, cid);
    return ResponseEntity.accepted()
        .header(HDR_CORRELATION_ID, cid)
        .body(res);
  }

  @GetMapping("/{cardId}")
  @Operation(summary = "Get card by id")
  public ResponseEntity<CardView> get(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("cardId") @NotBlank String cardId
  ) {
    String cid = ensureCorrelationId(correlationId);
    CardView view = service.get(cardId);
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .body(view);
  }

  @PostMapping("/{cardId}/activate")
  @Operation(summary = "Activate card")
  public ResponseEntity<CardView> activate(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("cardId") @NotBlank String cardId
  ) {
    String cid = ensureCorrelationId(correlationId);
    CardView view = service.activate(cardId, cid);
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .body(view);
  }

  @PostMapping("/{cardId}/status")
  @Operation(summary = "Update status (BLOCK|UNBLOCK)")
  public ResponseEntity<CardView> updateStatus(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("cardId") @NotBlank String cardId,
      @Valid @RequestBody UpdateStatusRequest request
  ) {
    String cid = ensureCorrelationId(correlationId);
    CardView view = service.updateStatus(cardId, request, cid);
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .body(view);
  }

  @PostMapping("/{cardId}/limits")
  @Operation(summary = "Update limits")
  public ResponseEntity<CardView> updateLimits(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("cardId") @NotBlank String cardId,
      @Valid @RequestBody UpdateLimitsRequest request
  ) {
    String cid = ensureCorrelationId(correlationId);
    CardView view = service.updateLimits(cardId, request, cid);
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .body(view);
  }

  @GetMapping("/customers/{customerId}")
  @Operation(summary = "List cards by customer")
  public ResponseEntity<List<CardView>> listByCustomer(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("customerId") Long customerId
  ) {
    String cid = ensureCorrelationId(correlationId);
    List<CardView> cards = service.listByCustomer(customerId);
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .body(cards);
  }

  @GetMapping("/issuance/{applicationId}")
  @Operation(summary = "Get card by issuance applicationId")
  public ResponseEntity<CardView> getByIssuanceApplication(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("applicationId") @NotBlank String applicationId
  ) {
    String cid = ensureCorrelationId(correlationId);
    CardView view = service.getByIssuanceApplication(applicationId);
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .body(view);
  }

  private static String ensureCorrelationId(String headerValue) {
    return StringUtils.hasText(headerValue) ? headerValue : java.util.UUID.randomUUID().toString();
  }
}
