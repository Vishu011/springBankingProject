package com.omnibank.loanmanagement.repository;

import com.omnibank.loanmanagement.domain.LoanAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanAccountRepository extends JpaRepository<LoanAccount, Long> {
  Optional<LoanAccount> findByLoanAccountNumber(String loanAccountNumber);
  Optional<LoanAccount> findByApplicationId(String applicationId);
  List<LoanAccount> findByCustomerId(Long customerId);
}
