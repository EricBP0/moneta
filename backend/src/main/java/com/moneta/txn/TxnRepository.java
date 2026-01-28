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
      and t.status = com.moneta.txn.TxnStatus.POSTED
      and t.isActive = true
    group by t.account.id
  """)
  List<TxnBalanceProjection> findPostedBalancesByUserId(@Param("userId") Long userId);

  @Query("""
    select coalesce(sum(case when t.direction = com.moneta.txn.TxnDirection.IN
      then t.amountCents else -t.amountCents end), 0)
    from Txn t
    where t.user.id = :userId
      and t.account.id = :accountId
      and t.status = com.moneta.txn.TxnStatus.POSTED
      and t.isActive = true
  """)
  Long findPostedBalanceByUserIdAndAccountId(
    @Param("userId") Long userId,
    @Param("accountId") Long accountId
  );

  interface TxnBalanceProjection {
    Long getAccountId();
    Long getBalanceCents();
  }
}
