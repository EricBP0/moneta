package com.moneta.budget;

import com.moneta.txn.TxnRepository;
import org.springframework.stereotype.Service;

@Service
public class BudgetCalculator {
  private final TxnRepository txnRepository;

  public BudgetCalculator(TxnRepository txnRepository) {
    this.txnRepository = txnRepository;
  }

  public long calculateConsumption(Long userId, String monthRef, Long categoryId, Long subcategoryId) {
    Long total = txnRepository.sumPostedOutByUserAndMonthAndCategory(userId, monthRef, categoryId, subcategoryId);
    return total == null ? 0L : total;
  }
}
