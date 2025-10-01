package com.omnibank.paymentgateway.repository;

import com.omnibank.paymentgateway.domain.PaymentRequest;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, Long> {
  Optional<PaymentRequest> findByPaymentUuid(String paymentUuid);
}
