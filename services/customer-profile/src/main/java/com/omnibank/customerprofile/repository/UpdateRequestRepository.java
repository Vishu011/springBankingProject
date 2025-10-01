package com.omnibank.customerprofile.repository;

import com.omnibank.customerprofile.domain.UpdateRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UpdateRequestRepository extends JpaRepository<UpdateRequest, Long> {
}
