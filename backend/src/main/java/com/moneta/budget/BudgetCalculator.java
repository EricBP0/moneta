package com.moneta.budget;

import com.moneta.txn.TxnRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BudgetCalculator {
  private static final Logger logger = LoggerFactory.getLogger(BudgetCalculator.class);

  private final TxnRepository txnRepository;

  public BudgetCalculator(TxnRepository txnRepository) {
    this.txnRepository = txnRepository;
  }

  public long calculateConsumption(Long userId, String monthRef, Long categoryId, Long subcategoryId) {
    Long total = txnRepository.sumSettledOutByUserAndMonthAndCategory(userId, monthRef, categoryId, subcategoryId);
    long count = txnRepository.countSettledOutByUserAndMonthAndCategory(userId, monthRef, categoryId, subcategoryId);
    long result = total == null ? 0L : total;
    logger.debug(
      "Budget consumption userId={} monthRef={} categoryId={} subcategoryId={} count={} totalCents={}",
      userId,
      monthRef,
      categoryId,
      subcategoryId,
      count,
      result
    );
    return result;
  }
}
