package com.omnibank.ledger.application;

import com.omnibank.ledger.application.LedgerService.Entry;
import com.omnibank.ledger.application.LedgerService.PostResult;
import com.omnibank.ledger.config.AppProperties;
import com.omnibank.ledger.domain.LedgerTransaction;
import com.omnibank.ledger.events.EventPublisher;
import com.omnibank.ledger.events.EventTypes;
import com.omnibank.ledger.repository.LedgerTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LedgerService double-entry validation and event publication.
 */
class LedgerServiceTest {

  private LedgerTransactionRepository txRepo;
  private EventPublisher eventPublisher;
  private AppProperties props;
  private LedgerService service;

  @BeforeEach
  void setUp() {
    txRepo = mock(LedgerTransactionRepository.class);
    eventPublisher = mock(EventPublisher.class);
    props = new AppProperties();
    props.getEvents().setTopic("ledger.events");

    // Return the argument as the saved entity
    when(txRepo.save(any(LedgerTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service = new LedgerService(txRepo, eventPublisher, props);
  }

  @Test
  void postTransaction_throwsWhenLessThanTwoEntries() {
    List<Entry> entries = List.of(new Entry("AC1", new BigDecimal("100.00"), 'D'));
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> service.postTransaction("TRANSFER", entries, "cid"));
    assertTrue(ex.getMessage().contains("At least two entries"));
  }

  @Test
  void postTransaction_throwsWhenSumsMismatch() {
    List<Entry> entries = List.of(
        new Entry("AC1", new BigDecimal("100.00"), 'D'),
        new Entry("AC2", new BigDecimal("90.00"), 'C')
    );
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> service.postTransaction("TRANSFER", entries, "cid"));
    assertTrue(ex.getMessage().contains("Sum of debits"));
  }

  @Test
  void postTransaction_throwsWhenInvalidDirection() {
    List<Entry> entries = List.of(
        new Entry("AC1", new BigDecimal("100.00"), 'X'),
        new Entry("AC2", new BigDecimal("100.00"), 'C')
    );
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> service.postTransaction("TRANSFER", entries, "cid"));
    assertTrue(ex.getMessage().contains("must be 'D' or 'C'"));
  }

  @Test
  void postTransaction_happyPath_postsAndPublishesEvent_withMetadata() {
    List<Entry> entries = List.of(
        new Entry("AC1", new BigDecimal("100.00"), 'D'),
        new Entry("AC2", new BigDecimal("100.00"), 'C')
    );

    Map<String, String> metadata = Map.of("loanAccountNumber", "LN1234567890");

    PostResult result = service.postTransaction("TRANSFER", entries, "cid", metadata);
    assertNotNull(result);
    assertEquals("POSTED", result.getStatus());
    assertNotNull(result.getTransactionId());
    assertFalse(result.getTransactionId().isBlank());

    ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
    verify(eventPublisher, times(1)).publish(
        eq("ledger.events"),
        eq(EventTypes.TRANSACTION_POSTED),
        payloadCaptor.capture(),
        eq("cid")
    );

    Object payload = payloadCaptor.getValue();
    assertNotNull(payload);
    // basic shape check via toString contains keys (we avoid strict JSON parsing here)
    String s = payload.toString();
    assertTrue(s.contains("transactionId"));
  }
}
