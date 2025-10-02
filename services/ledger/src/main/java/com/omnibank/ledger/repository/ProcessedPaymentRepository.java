package com.omnibank.ledger.repository;

import com.omnibank.ledger.domain.ProcessedPayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedPaymentRepository extends JpaRepository<ProcessedPayment, Long> {
  boolean existsByPaymentUuid(String paymentUuid);
}
