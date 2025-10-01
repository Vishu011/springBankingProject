package com.omnibank.onboarding.application;

import com.omnibank.onboarding.api.dto.AgentUpdateStatusRequest;
import com.omnibank.onboarding.api.dto.DocumentUploadRequest;
import com.omnibank.onboarding.api.dto.StartOnboardingRequest;
import com.omnibank.onboarding.config.AppProperties;
import com.omnibank.onboarding.domain.ApplicationState;
import com.omnibank.onboarding.domain.DocumentStatus;
import com.omnibank.onboarding.domain.OnboardingApplication;
import com.omnibank.onboarding.domain.OnboardingDocument;
import com.omnibank.onboarding.domain.VerificationRecord;
import com.omnibank.onboarding.events.EventPublisher;
import com.omnibank.onboarding.events.EventTypes;
import com.omnibank.onboarding.repository.OnboardingApplicationRepository;
import com.omnibank.onboarding.integration.CustomerProfileClient;
import lombok.extern.slf4j.Slf4j;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service containing orchestration and domain operations for onboarding.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingService {

  private final OnboardingApplicationRepository repository;
  private final EventPublisher eventPublisher;
  private final AppProperties props;
  private final CustomerProfileClient customerProfileClient;

  /**
   * Start a new onboarding application.
   */
  @Transactional
  public String start(StartOnboardingRequest req, String correlationId) {
    String applicationId = UUID.randomUUID().toString();
    OnboardingApplication app = OnboardingApplication.builder()
        .applicationId(applicationId)
        .firstName(req.getFirstName())
        .lastName(req.getLastName())
        .email(req.getEmail())
        .mobileNumber(req.getMobileNumber())
        .state(ApplicationState.STARTED)
        .build();

    repository.save(app);

    publish(EventTypes.APPLICATION_STARTED, new EventPayloads.ApplicationStarted(applicationId, req.getEmail()), correlationId);
    return applicationId;
  }

  /**
   * Attach documents, move to AWAITING_VERIFICATION, and signal VerificationRequired.
   */
  @Transactional
  public void uploadDocuments(String pathAppId, DocumentUploadRequest req, String correlationId) {
    if (pathAppId == null || pathAppId.isBlank()) {
      throw new IllegalArgumentException("Path applicationId is required");
    }
    OnboardingApplication app = repository.findByApplicationId(pathAppId)
        .orElseThrow(() -> new IllegalArgumentException("Application not found: " + pathAppId));

    // Append docs
    req.getDocuments().forEach(d ->
        app.getDocuments().add(OnboardingDocument.builder()
            .type(d.getType())
            .url(d.getUrl())
            .status(DocumentStatus.UPLOADED)
            .build())
    );

    // Transition state
    app.setState(ApplicationState.AWAITING_VERIFICATION);
    repository.save(app);

    publish(EventTypes.DOCUMENTS_UPLOADED, new EventPayloads.DocumentsUploaded(app.getApplicationId(), app.getDocuments().size()), correlationId);
    publish(EventTypes.VERIFICATION_REQUIRED, new EventPayloads.VerificationRequired(app.getApplicationId()), correlationId);
  }

  /**
   * Privileged update by an agent (human/admin) to advance or reject the application.
   */
  @Transactional
  public void agentUpdate(AgentUpdateStatusRequest req, String correlationId) {
    OnboardingApplication app = repository.findByApplicationId(req.getApplicationId())
        .orElseThrow(() -> new IllegalArgumentException("Application not found: " + req.getApplicationId()));

    // Record verification action if provided
    VerificationRecord.VerificationRecordBuilder rec = VerificationRecord.builder()
        .agent(req.getAgent())
        .timestamp(Instant.now())
        .action(req.getAction())
        .outcome(req.getOutcome())
        .confidenceScore(req.getConfidenceScore())
        .detailsJson(req.getDetailsJson());
    app.getVerificationHistory().add(rec.build());

    // Transition rules (simplified, aligns to design doc main states)
    ApplicationState newState = req.getNewState();
    switch (newState) {
      case VERIFIED, APPROVED -> {
        app.setState(newState);
        repository.save(app);
        // If final approval, emit OnboardingApproved
        if (newState == ApplicationState.APPROVED) {
          publish(EventTypes.ONBOARDING_APPROVED,
              new EventPayloads.OnboardingApproved(app.getApplicationId(), app.getFirstName(), app.getLastName(), app.getEmail(), app.getMobileNumber()),
              correlationId);
          // Dev wiring: call customer-profile to create CIF
          try {
            customerProfileClient.sendOnboardingApproved(
                app.getFirstName(),
                app.getLastName(),
                app.getEmail(),
                app.getMobileNumber(),
                app.getApplicationId(),
                correlationId
            );
          } catch (Exception ex) {
            log.warn("Dev wiring to customer-profile failed: {}", ex.getMessage());
          }
        }
      }
      case FLAGGED_FOR_MANUAL_REVIEW -> {
        app.setState(ApplicationState.FLAGGED_FOR_MANUAL_REVIEW);
        repository.save(app);
        publish(EventTypes.MANUAL_REVIEW_REQUIRED,
            new EventPayloads.ManualReviewRequired(app.getApplicationId(), req.getJustification()),
            correlationId);
      }
      case REJECTED -> {
        app.setState(ApplicationState.REJECTED);
        repository.save(app);
        // No specific event required by doc, but can be added if needed.
      }
      default -> throw new IllegalArgumentException("Unsupported state transition to: " + newState);
    }
  }

  private void publish(String type, Object payload, String correlationId) {
    eventPublisher.publish(props.getEvents().getTopic(), type, payload, correlationId);
  }

  // Event payloads grouped for convenience
  public static class EventPayloads {
    public record ApplicationStarted(String applicationId, String email) {}
    public record DocumentsUploaded(String applicationId, int documentsCount) {}
    public record VerificationRequired(String applicationId) {}
    public record OnboardingApproved(String applicationId, String firstName, String lastName, String email, String mobileNumber) {}
    public record ManualReviewRequired(String applicationId, String reason) {}
  }
}
