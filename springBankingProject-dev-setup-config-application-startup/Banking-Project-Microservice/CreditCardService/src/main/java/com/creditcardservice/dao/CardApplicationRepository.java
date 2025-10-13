package com.creditcardservice.dao;

import com.creditcardservice.model.CardApplication;
import com.creditcardservice.model.CardApplication.ApplicationStatus;
import com.creditcardservice.model.CardKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardApplicationRepository extends JpaRepository<CardApplication, String> {

    List<CardApplication> findByUserIdOrderBySubmittedAtDesc(String userId);

    List<CardApplication> findByStatusOrderBySubmittedAtAsc(ApplicationStatus status);

    long countByAccountIdAndTypeAndStatusIn(String accountId, CardKind type, List<ApplicationStatus> statuses);
}
