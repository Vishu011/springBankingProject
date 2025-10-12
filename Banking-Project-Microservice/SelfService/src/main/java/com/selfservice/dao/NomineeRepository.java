package com.selfservice.dao;

import com.selfservice.model.Nominee;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NomineeRepository extends JpaRepository<Nominee, String> {
    List<Nominee> findByUserIdOrderByCreatedAtDesc(String userId);
}
