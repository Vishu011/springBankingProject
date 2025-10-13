package com.selfservice.dao;

import com.selfservice.model.SelfServiceRequest;
import com.selfservice.model.SelfServiceRequestStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SelfServiceRequestRepository extends JpaRepository<SelfServiceRequest, String> {
    List<SelfServiceRequest> findByUserIdOrderBySubmittedAtDesc(String userId);
    List<SelfServiceRequest> findByStatusOrderBySubmittedAtAsc(SelfServiceRequestStatus status);
}
