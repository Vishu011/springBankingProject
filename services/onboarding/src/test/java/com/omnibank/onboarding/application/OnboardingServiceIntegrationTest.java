package com.omnibank.onboarding.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.omnibank.onboarding.api.dto.AgentUpdateStatusRequest;
import com.omnibank.onboarding.api.dto.DocumentUploadRequest;
import com.omnibank.onboarding.api.dto.StartOnboardingRequest;
import com.omnibank.onboarding.config.AppProperties;
import com.omnibank.onboarding.domain.ApplicationState;
import com.omnibank.onboarding.domain.OnboardingApplication;
import com.omnibank.onboarding.domain.OnboardingDocument;
import com.omnibank.onboarding.events.EventPublisher;
import com.omnibank.onboarding.events.EventTypes;
import com.omnibank.onboarding.repository.OnboardingApplicationRepository;
import com.omnibank.onboarding.integration.CustomerProfileClient;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceIntegrationTest {

  @Mock
  private OnboardingApplicationRepository repository;

  @Mock
  private EventPublisher eventPublisher;

  @Mock
  private CustomerProfileClient customerProfileClient;

  private AppProperties props;
  private OnboardingService service;

  @BeforeEach
  void setup() {
    props = new AppProperties();
    service = new OnboardingService(repository, eventPublisher, props, customerProfileClient);
  }

  @Test
  void start_createsApplication_andPublishesEvent() {
    // capture the entity saved
    ArgumentCaptor<OnboardingApplication> savedCaptor = ArgumentCaptor.forClass(OnboardingApplication.class);
    doAnswer(inv -> inv.getArgument(0)).when(repository).save(any(OnboardingApplication.class));

    StartOnboardingRequest req = new StartOnboardingRequest();
    req.setFirstName("John");
    req.setLastName("Doe");
    req.setEmail("john.doe@example.com");
    req.setMobileNumber("9999999999");

    String appId = service.start(req, "cid-1");

    assertThat(appId).isNotBlank();
    verify(repository, times(1)).save(savedCaptor.capture());
    OnboardingApplication saved = savedCaptor.getValue();
    assertThat(saved.getState()).isEqualTo(ApplicationState.STARTED);
    assertThat(saved.getFirstName()).isEqualTo("John");

    verify(eventPublisher, times(1))
        .publish(eq("onboarding.events"), eq(EventTypes.APPLICATION_STARTED), any(), eq("cid-1"));
  }

  @Test
  void uploadDocuments_movesToAwaitingVerification_andPublishesEvents() {
    String appId = UUID.randomUUID().toString();
    OnboardingApplication existing = OnboardingApplication.builder()
        .applicationId(appId)
        .state(ApplicationState.STARTED)
        .build();
    when(repository.findByApplicationId(appId)).thenReturn(Optional.of(existing));
    doAnswer(inv -> inv.getArgument(0)).when(repository).save(any(OnboardingApplication.class));

    DocumentUploadRequest docReq = new DocumentUploadRequest();

    DocumentUploadRequest.DocumentItem d1 = new DocumentUploadRequest.DocumentItem();
    d1.setType("PASSPORT");
    d1.setUrl("http://files/passport.png");
    DocumentUploadRequest.DocumentItem d2 = new DocumentUploadRequest.DocumentItem();
    d2.setType("UTILITY_BILL");
    d2.setUrl("http://files/utility.pdf");
    docReq.setDocuments(List.of(d1, d2));

    service.uploadDocuments(appId, docReq, "cid-2");

    assertThat(existing.getState()).isEqualTo(ApplicationState.AWAITING_VERIFICATION);
    assertThat(existing.getDocuments()).hasSize(2);

    verify(eventPublisher, times(1))
        .publish(eq("onboarding.events"), eq(EventTypes.DOCUMENTS_UPLOADED), any(), eq("cid-2"));
    verify(eventPublisher, times(1))
        .publish(eq("onboarding.events"), eq(EventTypes.VERIFICATION_REQUIRED), any(), eq("cid-2"));
  }

  @Test
  void agentUpdate_toApproved_emitsOnboardingApproved() {
    String appId = UUID.randomUUID().toString();
    OnboardingApplication existing = OnboardingApplication.builder()
        .applicationId(appId)
        .state(ApplicationState.AWAITING_VERIFICATION)
        .build();
    when(repository.findByApplicationId(appId)).thenReturn(Optional.of(existing));
    doAnswer(inv -> inv.getArgument(0)).when(repository).save(any(OnboardingApplication.class));

    AgentUpdateStatusRequest upd = new AgentUpdateStatusRequest();
    upd.setApplicationId(appId);
    upd.setNewState(ApplicationState.APPROVED);
    upd.setAgent("admin.user");
    upd.setAction("FINAL_DECISION");
    upd.setOutcome("PASS");
    upd.setJustification("All checks passed");

    service.agentUpdate(upd, "cid-3");

    assertThat(existing.getState()).isEqualTo(ApplicationState.APPROVED);

    verify(eventPublisher, times(1))
        .publish(eq("onboarding.events"), eq(EventTypes.ONBOARDING_APPROVED), any(), eq("cid-3"));
  }
}
