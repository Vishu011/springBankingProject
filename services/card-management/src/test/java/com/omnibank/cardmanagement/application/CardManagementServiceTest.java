package com.omnibank.cardmanagement.application;

import com.omnibank.cardmanagement.config.AppProperties;
import com.omnibank.cardmanagement.events.EventPublisher;
import com.omnibank.cardmanagement.events.EventTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

public class CardManagementServiceTest {

  private EventPublisher publisher;
  private AppProperties props;
  private CardManagementService service;

  @BeforeEach
  void setUp() {
    publisher = Mockito.mock(EventPublisher.class);
    props = new AppProperties();
    service = new CardManagementService(publisher, props);
  }

  @Test
  void createDev_successPublishesAndReturnsPending() {
    CardManagementService.CreateCardRequest req = new CardManagementService.CreateCardRequest();
    req.customerId = 123L;
    req.productType = "CREDIT_CARD";
    req.initialLimit = new BigDecimal("5000.00");

    String cid = "test-cid";
    CardManagementService.Created created = service.createDev(req, cid);

    assertNotNull(created);
    assertNotNull(created.cardId());
    assertEquals("PENDING", created.status());
    verify(publisher).publish(eq(props.getEvents().getTopic()), eq(EventTypes.CARD_CREATED), any(), eq(cid));
  }

  @Test
  void activate_blockedCardShouldFail() {
    // create
    CardManagementService.CreateCardRequest req = new CardManagementService.CreateCardRequest();
    req.customerId = 456L;
    req.initialLimit = new BigDecimal("1200.00");
    CardManagementService.Created created = service.createDev(req, "cid-1");

    // block
    CardManagementService.UpdateStatusRequest blockReq = new CardManagementService.UpdateStatusRequest();
    blockReq.status = "BLOCK";
    CardManagementService.CardView blocked = service.updateStatus(created.cardId(), blockReq, "cid-2");
    assertEquals("BLOCKED", blocked.status());

    // activate should fail
    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.activate(created.cardId(), "cid-3"));
    assertTrue(ex.getMessage().toLowerCase().contains("cannot activate"));
  }

  @Test
  void status_blockThenUnblockTransitionsAndPublishes() {
    CardManagementService.CreateCardRequest req = new CardManagementService.CreateCardRequest();
    req.customerId = 789L;
    CardManagementService.Created created = service.createDev(req, "cid-a");

    // BLOCK
    CardManagementService.UpdateStatusRequest blockReq = new CardManagementService.UpdateStatusRequest();
    blockReq.status = "BLOCK";
    CardManagementService.CardView blocked = service.updateStatus(created.cardId(), blockReq, "cid-b");
    assertEquals("BLOCKED", blocked.status());
    verify(publisher).publish(eq(props.getEvents().getTopic()), eq(EventTypes.CARD_STATUS_UPDATED), any(), eq("cid-b"));

    // UNBLOCK (-> ACTIVE)
    CardManagementService.UpdateStatusRequest unblockReq = new CardManagementService.UpdateStatusRequest();
    unblockReq.status = "UNBLOCK";
    CardManagementService.CardView active = service.updateStatus(created.cardId(), unblockReq, "cid-c");
    assertEquals("ACTIVE", active.status());
    verify(publisher).publish(eq(props.getEvents().getTopic()), eq(EventTypes.CARD_STATUS_UPDATED), any(), eq("cid-c"));
  }

  @Test
  void updateLimits_invalidShouldFail() {
    CardManagementService.CreateCardRequest req = new CardManagementService.CreateCardRequest();
    req.customerId = 222L;
    CardManagementService.Created created = service.createDev(req, "cid-lim-1");

    CardManagementService.UpdateLimitsRequest bad = new CardManagementService.UpdateLimitsRequest();
    bad.limit = new BigDecimal("0.00");

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.updateLimits(created.cardId(), bad, "cid-lim-2"));
    assertTrue(ex.getMessage().toLowerCase().contains("limit"));
  }

  @Test
  void updateLimits_successPublishes() {
    CardManagementService.CreateCardRequest req = new CardManagementService.CreateCardRequest();
    req.customerId = 333L;
    CardManagementService.Created created = service.createDev(req, "cid-lim-3");

    CardManagementService.UpdateLimitsRequest ok = new CardManagementService.UpdateLimitsRequest();
    ok.limit = new BigDecimal("9000.00");
    CardManagementService.CardView view = service.updateLimits(created.cardId(), ok, "cid-lim-4");

    assertNotNull(view);
    assertEquals(0, view.spendLimit().compareTo(new BigDecimal("9000.00")));
    verify(publisher).publish(eq(props.getEvents().getTopic()), eq(EventTypes.CARD_LIMITS_CHANGED), any(), eq("cid-lim-4"));
  }
}
