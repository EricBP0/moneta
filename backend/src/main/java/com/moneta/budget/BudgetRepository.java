package com.moneta.budget;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
  List<Budget> findAllByUserIdAndMonthRef(Long userId, String monthRef);

  List<Budget> findAllByUserId(Long userId);

  Optional<Budget> findByIdAndUserId(Long id, Long userId);

  @Query("""
    select b from Budget b
    where b.user.id = :userId
      and b.monthRef = :monthRef
      and ((:categoryId is null and b.categoryId is null) or b.categoryId = :categoryId)
      and ((:subcategoryId is null and b.subcategoryId is null) or b.subcategoryId = :subcategoryId)
  """)
  Optional<Budget> findExisting(
    @Param("userId") Long userId,
    @Param("monthRef") String monthRef,
    @Param("categoryId") Long categoryId,
    @Param("subcategoryId") Long subcategoryId
  );
}
