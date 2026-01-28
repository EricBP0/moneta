package com.moneta.goal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoalContributionRepository extends JpaRepository<GoalContribution, Long> {
  Optional<GoalContribution> findByIdAndUserIdAndGoalId(Long id, Long userId, Long goalId);

  @Query("""
    select coalesce(sum(gc.amountCents), 0)
    from GoalContribution gc
    where gc.user.id = :userId
      and gc.goal.id = :goalId
  """)
  Long sumByGoalId(@Param("userId") Long userId, @Param("goalId") Long goalId);

  @Query("""
    select coalesce(sum(gc.amountCents), 0)
    from GoalContribution gc
    where gc.user.id = :userId
      and gc.goal.id = :goalId
      and gc.contributedAt <= :endDate
  """)
  Long sumByGoalIdUpTo(@Param("userId") Long userId, @Param("goalId") Long goalId, @Param("endDate") LocalDate endDate);

  @Query("""
    select gc.goal.id as goalId, coalesce(sum(gc.amountCents), 0) as totalCents
    from GoalContribution gc
    where gc.user.id = :userId
      and gc.goal.id in :goalIds
    group by gc.goal.id
  """)
  List<GoalContributionTotalProjection> sumTotalsByGoalIds(
    @Param("userId") Long userId,
    @Param("goalIds") List<Long> goalIds
  );

  @Query("""
    select gc
    from GoalContribution gc
    where gc.user.id = :userId
      and gc.goal.id = :goalId
      and (:fromDate is null or gc.contributedAt >= :fromDate)
      and (:toDate is null or gc.contributedAt <= :toDate)
    order by gc.contributedAt desc, gc.id desc
  """)
  Page<GoalContribution> findAllByFilters(
    @Param("userId") Long userId,
    @Param("goalId") Long goalId,
    @Param("fromDate") LocalDate fromDate,
    @Param("toDate") LocalDate toDate,
    Pageable pageable
  );

  interface GoalContributionTotalProjection {
    Long getGoalId();
    Long getTotalCents();
  }
}
