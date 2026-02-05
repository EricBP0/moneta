package com.moneta.card;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Long> {
  @EntityGraph(attributePaths = {"account"})
  List<Card> findAllByUserIdAndIsActiveTrue(Long userId);
  
  Optional<Card> findByIdAndUserId(Long id, Long userId);
  
  @EntityGraph(attributePaths = {"account"})
  Optional<Card> findByIdAndUserIdAndIsActiveTrue(Long id, Long userId);
  
  Optional<Card> findByUserIdAndNameIgnoreCaseAndIsActiveTrue(Long userId, String name);
}
