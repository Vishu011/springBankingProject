package com.omnibank.customerprofile.repository;

import com.omnibank.customerprofile.domain.Customer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
  Optional<Customer> findByCifId(String cifId);
}
