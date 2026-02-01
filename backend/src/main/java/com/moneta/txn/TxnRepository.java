package com.moneta.txn;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TxnRepository extends JpaRepository<Txn, Long>, JpaSpecificationExecutor<Txn> {
  Optional<Txn> findByIdAndUserIdAndIsActiveTrue(Long id, Long userId);
  List<Txn> findByUserIdAndAccountIdAndIsActiveTrue(Long userId, Long accountId);

  @Query("""
    select t.account.id as accountId,
      coalesce(sum(case when t.direction = com.moneta.txn.TxnDirection.IN
        then t.amountCents else -t.amountCents end), 0) as balanceCents
    from Txn t
    where t.user.id = :userId
      and t.status in (com.moneta.txn.TxnStatus.POSTED, com.moneta.txn.TxnStatus.CLEARED)
      and t.isActive = true
    group by t.account.id
  """)
  List<TxnBalanceProjection> findSettledBalancesByUserId(@Param("userId") Long userId);

  @Query("""
    select coalesce(sum(case when t.direction = com.moneta.txn.TxnDirection.IN
      then t.amountCents else -t.amountCents end), 0)
    from Txn t
    where t.user.id = :userId
      and t.account.id = :accountId
      and t.status in (com.moneta.txn.TxnStatus.POSTED, com.moneta.txn.TxnStatus.CLEARED)
      and t.isActive = true
  """)
  Long findSettledBalanceByUserIdAndAccountId(
    @Param("userId") Long userId,
    @Param("accountId") Long accountId
  );

  @Query("""
    select coalesce(sum(t.amountCents), 0)
    from Txn t
    where t.user.id = :userId
      and t.monthRef = :monthRef
      and t.status in (com.moneta.txn.TxnStatus.POSTED, com.moneta.txn.TxnStatus.CLEARED)
      and t.direction = com.moneta.txn.TxnDirection.OUT
      and t.isActive = true
      and (:categoryId is null or t.categoryId = :categoryId)
      and (:subcategoryId is null or t.subcategoryId = :subcategoryId)
  """)
  Long sumSettledOutByUserAndMonthAndCategory(
    @Param("userId") Long userId,
    @Param("monthRef") String monthRef,
    @Param("categoryId") Long categoryId,
    @Param("subcategoryId") Long subcategoryId
  );

  @Query("""
    select count(t)
    from Txn t
    where t.user.id = :userId
      and t.monthRef = :monthRef
      and t.status in (com.moneta.txn.TxnStatus.POSTED, com.moneta.txn.TxnStatus.CLEARED)
      and t.isActive = true
  """)
  long countSettledByUserIdAndMonthRef(
    @Param("userId") Long userId,
    @Param("monthRef") String monthRef
  );

  @Query("""
    select count(t)
    from Txn t
    where t.user.id = :userId
      and t.monthRef = :monthRef
      and t.status in (com.moneta.txn.TxnStatus.POSTED, com.moneta.txn.TxnStatus.CLEARED)
      and t.direction = com.moneta.txn.TxnDirection.OUT
      and t.isActive = true
      and (:categoryId is null or t.categoryId = :categoryId)
      and (:subcategoryId is null or t.subcategoryId = :subcategoryId)
  """)
  long countSettledOutByUserAndMonthAndCategory(
    @Param("userId") Long userId,
    @Param("monthRef") String monthRef,
    @Param("categoryId") Long categoryId,
    @Param("subcategoryId") Long subcategoryId
  );

  /**
   * Finds monthly totals (income and expenses) for a user in a specific month.
   * Includes transactions with status POSTED or CLEARED.
   *
   * @param userId the user ID
   * @param monthRef the month reference (format: YYYY-MM)
   * @return monthly totals projection with income and expense cents
   */
  @Query("""
    select
      coalesce(sum(case when t.direction = com.moneta.txn.TxnDirection.IN then t.amountCents else 0 end), 0)
        as incomeCents,
      coalesce(sum(case when t.direction = com.moneta.txn.TxnDirection.OUT then t.amountCents else 0 end), 0)
        as expenseCents
    from Txn t
    where t.user.id = :userId
      and t.monthRef = :monthRef
      and t.status in (com.moneta.txn.TxnStatus.POSTED, com.moneta.txn.TxnStatus.CLEARED)
      and t.isActive = true
  """)
  MonthlyTotalsProjection findMonthlyTotals(
    @Param("userId") Long userId,
    @Param("monthRef") String monthRef
  );

  /**
   * Finds category-wise expenses for a user in a specific month.
   * Includes transactions with status POSTED or CLEARED.
   *
   * @param userId the user ID
   * @param monthRef the month reference (format: YYYY-MM)
   * @return list of category expense projections
   */
  @Query("""
    select t.categoryId as categoryId,
      coalesce(sum(t.amountCents), 0) as expenseCents
    from Txn t
    where t.user.id = :userId
      and t.monthRef = :monthRef
      and t.status in (com.moneta.txn.TxnStatus.POSTED, com.moneta.txn.TxnStatus.CLEARED)
      and t.direction = com.moneta.txn.TxnDirection.OUT
      and t.isActive = true
      and t.categoryId is not null
    group by t.categoryId
  """)
  List<CategoryExpenseProjection> findCategoryExpenses(
    @Param("userId") Long userId,
    @Param("monthRef") String monthRef
  );

  interface TxnBalanceProjection {
    Long getAccountId();
    Long getBalanceCents();
  }

  interface MonthlyTotalsProjection {
    Long getIncomeCents();
    Long getExpenseCents();
  }

  interface CategoryExpenseProjection {
    Long getCategoryId();
    Long getExpenseCents();
  }
}
