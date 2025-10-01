package com.omnibank.ledger.repository;

import com.omnibank.ledger.domain.LedgerTransaction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, Long> {
  Optional<LedgerTransaction> findByTransactionUuid(String transactionUuid);
}
