package com.omnibank.investmentonboarding.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OnboardingService {

  private final ConcurrentHashMap<String, Profile> store = new ConcurrentHashMap<>();

  public View start(@NotNull Long customerId,
                    @NotBlank String riskLevel,
                    Map<String, Object> answers,
                    String correlationId) {
    if (customerId == null || customerId <= 0) {
      throw new IllegalArgumentException("customerId must be provided");
    }
    if (!StringUtils.hasText(riskLevel)) {
      throw new IllegalArgumentException("riskLevel is required");
    }
    String id = UUID.randomUUID().toString();
    Profile p = new Profile(id, customerId, riskLevel.toUpperCase(), "NOT_STARTED", answers, Instant.now(), null);
    store.put(id, p);
    // Minimal state transition: NOT_STARTED -> ACTIVE on activation API
    return toView(p);
  }

  public View activate(@NotBlank String profileId, String correlationId) {
    Profile p = required(profileId);
    if (!"ACTIVE".equalsIgnoreCase(p.status())) {
      p = new Profile(p.id(), p.customerId(), p.riskLevel(), "ACTIVE", p.answers(), p.createdAt(), Instant.now());
      store.put(p.id(), p);
    }
    return toView(p);
  }

  public View get(@NotBlank String profileId) {
    return toView(required(profileId));
  }

  private Profile required(String id) {
    Profile p = store.get(id);
    if (p == null) throw new IllegalArgumentException("Profile not found: " + id);
    return p;
  }

  private static View toView(Profile p) {
    return new View(p.id(), p.customerId(), p.riskLevel(), p.status(), p.createdAt(), p.activatedAt());
  }

  public record Profile(
      String id,
      Long customerId,
      String riskLevel, // CONSERVATIVE | BALANCED | AGGRESSIVE
      String status,    // NOT_STARTED | ACTIVE
      Map<String, Object> answers,
      Instant createdAt,
      Instant activatedAt
  ) {}

  public record View(
      String profileId,
      Long customerId,
      String riskLevel,
      String status,
      Instant createdAt,
      Instant activatedAt
  ) {}
}
