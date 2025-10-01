package com.omnibank.customerprofile.repository;

import com.omnibank.customerprofile.domain.Address;
import com.omnibank.customerprofile.domain.Customer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long> {
  List<Address> findByCustomerOrderByCreatedAtDesc(Customer customer);
}
