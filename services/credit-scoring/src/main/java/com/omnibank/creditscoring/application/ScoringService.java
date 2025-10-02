package com.omnibank.creditscoring.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Dev-local mock scoring logic.
 * Produces deterministic values from applicantId for repeatable tests.
 */
@Service
public class ScoringService {

  public ScoringResult calculate(String applicantId) {
    int seed = Math.abs(applicantId != null ? applicantId.hashCode() : 0);
    // Simple deterministic mock:
    // Map seed to a score range 300..850
    int score = 300 + (seed % 551); // 300-850
    // PD inversely related to score (roughly)
    BigDecimal pd = BigDecimal.valueOf(1.0 - (score - 300) / 550.0)
        .setScale(4, RoundingMode.HALF_UP);

    List<String> keyFactors = List.of(
        (score > 740 ? "Low utilization" : "High utilization"),
        (score > 700 ? "Long credit history" : "Limited credit history"),
        (score > 680 ? "On-time payments" : "Recent late payments")
    );

    // Minimal bureau-like data stub
    BureauData bureau = new BureauData(
        score,
        (seed % 5) + 1,                 // openAccounts
        (seed % 3),                     // delinquencies
        BigDecimal.valueOf((seed % 80) * 1000L) // outstandingDebt
    );

    return new ScoringResult(score, pd, keyFactors, bureau);
  }

  public record ScoringResult(
      int creditScore,
      BigDecimal probabilityOfDefault,
      List<String> keyFactors,
      BureauData bureauData
  ) {}

  public record BureauData(
      int score,
      int openAccounts,
      int delinquencies,
      BigDecimal outstandingDebt
  ) {}
}
