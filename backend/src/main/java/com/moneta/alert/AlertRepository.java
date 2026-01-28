package com.moneta.alert;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {
  List<Alert> findAllByUserIdAndMonthRef(Long userId, String monthRef);

  List<Alert> findAllByUserId(Long userId);

  Optional<Alert> findByIdAndUserId(Long id, Long userId);

  boolean existsByUserIdAndBudgetIdAndMonthRefAndType(Long userId, Long budgetId, String monthRef, AlertType type);
}
