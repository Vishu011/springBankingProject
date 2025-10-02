package com.omnibank.loanmanagement.repository;

import com.omnibank.loanmanagement.domain.LoanEmiApplied;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanEmiAppliedRepository extends JpaRepository<LoanEmiApplied, Long> {
  boolean existsByTransactionId(String transactionId);
}
