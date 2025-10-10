package com.creditcardservice.dao;

import com.creditcardservice.model.Card;
import com.creditcardservice.model.CardKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, String> {

    boolean existsByCardNumber(String cardNumber);

    List<Card> findByUserId(String userId);

    Optional<Card> findByCardNumber(String cardNumber);

    long countByAccountIdAndType(String accountId, CardKind type);
}
