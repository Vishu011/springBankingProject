package com.omnibank.beneficiary.repository;

import com.omnibank.beneficiary.domain.Beneficiary;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
  List<Beneficiary> findByOwningCustomerIdOrderByCreatedAtDesc(Long owningCustomerId);
  Optional<Beneficiary> findByIdAndOwningCustomerId(Long beneficiaryId, Long owningCustomerId);

  // for cooling-off check (optional convenience)
  default boolean withinCoolingOff(Beneficiary b, long hours) {
    if (b.getAddedTs() == null) return true;
    Instant cutoff = b.getAddedTs().plusSeconds(hours * 3600);
    return Instant.now().isBefore(cutoff);
  }
}
