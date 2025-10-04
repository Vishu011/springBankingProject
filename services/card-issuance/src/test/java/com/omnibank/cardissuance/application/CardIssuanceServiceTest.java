package com.omnibank.cardissuance.application;

import com.omnibank.cardissuance.config.AppProperties;
import com.omnibank.cardissuance.events.EventPublisher;
import com.omnibank.cardissuance.events.EventTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class CardIssuanceServiceTest {

  private EventPublisher publisher;
  private AppProperties props;
  private CardIssuanceService service;

  @BeforeEach
  void setUp() {
    publisher = Mockito.mock(EventPublisher.class);
    props = new AppProperties();
    service = new CardIssuanceService(publisher, props);
  }

  @Test
  void submit_success_createsApplicationWithSubmittedStatus() {
    CardIssuanceService.SubmitRequest req = new CardIssuanceService.SubmitRequest();
    req.customerId = 101L;
    req.productType = "CREDIT_CARD";

    String cid = "cid-submit-1";
    CardIssuanceService.Created created = service.submit(req, cid);

    assertNotNull(created);
    assertNotNull(created.applicationId());
    assertEquals("SUBMITTED", created.status());

    // submit should not publish any event yet
    verifyNoInteractions(publisher);
  }

  @Test
  void eligibility_then_approve_publishesApprovedEvent() {
    // submit
    CardIssuanceService.SubmitRequest req = new CardIssuanceService.SubmitRequest();
    req.customerId = 202L;
    CardIssuanceService.Created created = service.submit(req, "cid-flow-1");

    // eligibility check
    CardIssuanceService.ApplicationView afterElig = service.eligibilityCheck(created.applicationId(), "cid-flow-2");
    assertEquals("ELIGIBILITY_CHECKED", afterElig.status());

    // approve
    String cidApprove = "cid-flow-3";
    CardIssuanceService.ApplicationView afterApprove = service.approve(created.applicationId(), cidApprove);
    assertEquals("APPROVED", afterApprove.status());

    verify(publisher).publish(
        eq(props.getEvents().getTopic()),
        eq(EventTypes.CARD_APPLICATION_APPROVED),
        any(),
        eq(cidApprove)
    );
  }

  @Test
  void approve_directly_from_submitted_is_allowed_and_publishes() {
    CardIssuanceService.SubmitRequest req = new CardIssuanceService.SubmitRequest();
    req.customerId = 303L;
    CardIssuanceService.Created created = service.submit(req, "cid-direct-1");

    String cid = "cid-direct-2";
    CardIssuanceService.ApplicationView approved = service.approve(created.applicationId(), cid);
    assertEquals("APPROVED", approved.status());

    verify(publisher).publish(
        eq(props.getEvents().getTopic()),
        eq(EventTypes.CARD_APPLICATION_APPROVED),
        any(),
        eq(cid)
    );
  }

  @Test
  void get_nonexistent_throws() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.get("nope"));
    assertTrue(ex.getMessage().contains("Application not found"));
  }

  @Test
  void submit_invalid_customer_rejected() {
    CardIssuanceService.SubmitRequest bad = new CardIssuanceService.SubmitRequest();
    bad.customerId = 0L;

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.submit(bad, "cid-bad"));
    assertTrue(ex.getMessage().toLowerCase().contains("customerid"));
  }
}
