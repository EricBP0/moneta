package com.moneta.card;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Long> {
  List<Card> findAllByUserIdAndActiveTrue(Long userId);
  Optional<Card> findByIdAndUserId(Long id, Long userId);
  Optional<Card> findByIdAndUserIdAndActiveTrue(Long id, Long userId);
}
