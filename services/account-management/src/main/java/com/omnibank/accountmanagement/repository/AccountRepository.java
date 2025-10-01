package com.omnibank.accountmanagement.repository;

import com.omnibank.accountmanagement.domain.Account;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {
  List<Account> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
