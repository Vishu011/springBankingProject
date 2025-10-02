package com.omnibank.ledger.repository;

import com.omnibank.ledger.domain.LedgerEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Query access for ledger entries.
 */
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

  /**
   * Returns all entries for an account, newest first based on the parent transaction's posted timestamp.
   * Includes the parent transaction in the persistence context for access to postedTs and uuid.
   */
  @Query("""
      select e
      from LedgerEntry e
      join fetch e.transaction t
      where e.accountNumber = :account
      order by t.postedTs desc, t.id desc, e.id desc
      """)
  List<LedgerEntry> findHistoryByAccount(@Param("account") String account);
}
