package com.omnibank.onboarding.repository;

import com.omnibank.onboarding.domain.OnboardingApplication;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OnboardingApplicationRepository extends MongoRepository<OnboardingApplication, String> {
  Optional<OnboardingApplication> findByApplicationId(String applicationId);
  boolean existsByApplicationId(String applicationId);
}
