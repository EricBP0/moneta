package com.moneta.card;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<Card, Long> {
  List<Card> findAllByUserIdAndIsActiveTrue(Long userId);
  Optional<Card> findByIdAndUserId(Long id, Long userId);
  Optional<Card> findByIdAndUserIdAndIsActiveTrue(Long id, Long userId);
}
