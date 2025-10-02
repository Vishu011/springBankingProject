package com.omnibank.loanmanagement.repository;

import com.omnibank.loanmanagement.domain.LoanAccount;
import com.omnibank.loanmanagement.domain.LoanSchedule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanScheduleRepository extends JpaRepository<LoanSchedule, Long> {
  List<LoanSchedule> findByLoanAccountOrderBySeqNoAsc(LoanAccount loanAccount);
}
