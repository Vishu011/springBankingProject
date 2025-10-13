package com.accountMicroservice.dao;

import com.accountMicroservice.model.SalaryAccountApplication;
import com.accountMicroservice.model.SalaryApplicationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaryAccountApplicationRepository extends JpaRepository<SalaryAccountApplication, String> {

    List<SalaryAccountApplication> findByUserIdOrderBySubmittedAtDesc(String userId);

    List<SalaryAccountApplication> findByStatusOrderBySubmittedAtDesc(SalaryApplicationStatus status);

    Optional<SalaryAccountApplication> findByApplicationId(String applicationId);
}
