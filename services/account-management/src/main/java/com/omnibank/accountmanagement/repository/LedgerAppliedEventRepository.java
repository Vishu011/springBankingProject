package com.omnibank.accountmanagement.repository;

import com.omnibank.accountmanagement.domain.LedgerAppliedEvent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerAppliedEventRepository extends JpaRepository<LedgerAppliedEvent, Long> {
  Optional<LedgerAppliedEvent> findByTransactionId(String transactionId);
  boolean existsByTransactionId(String transactionId);
}
