package com.omnibank.onboarding.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Root aggregate for customer onboarding applications.
 * Stored in MongoDB per the design document.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "onboarding_applications")
public class OnboardingApplication {

  @Id
  private String id; // Mongo ObjectId as String

  /**
   * Stable external identifier (UUID string) used in APIs and events.
   */
  private String applicationId;

  // Applicant basic info (ingested at start)
  private String firstName;
  private String lastName;
  private String email;
  private String mobileNumber;

  private ApplicationState state;

  @Builder.Default
  private List<OnboardingDocument> documents = new ArrayList<>();

  @Builder.Default
  private List<VerificationRecord> verificationHistory = new ArrayList<>();

  @CreatedDate
  private Instant createdAt;

  @LastModifiedDate
  private Instant updatedAt;
}
