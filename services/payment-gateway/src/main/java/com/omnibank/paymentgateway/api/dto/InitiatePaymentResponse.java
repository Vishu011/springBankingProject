package com.omnibank.paymentgateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for payment initiation requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitiatePaymentResponse {
  private String paymentId;
  private String status;
}
