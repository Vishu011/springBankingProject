package com.userMicroservice.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.userMicroservice.model.KycApplication;
import com.userMicroservice.model.KycReviewStatus;

public interface KycApplicationRepository extends JpaRepository<KycApplication, String> {
    Optional<KycApplication> findTopByUserIdOrderBySubmittedAtDesc(String userId);
    List<KycApplication> findByReviewStatus(KycReviewStatus status);
}
