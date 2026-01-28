package com.moneta.alert;

import com.moneta.budget.Budget;
import com.moneta.budget.BudgetCalculator;
import com.moneta.budget.BudgetRepository;
import com.moneta.common.MonthRefValidator;
import com.moneta.txn.Txn;
import com.moneta.txn.TxnDirection;
import com.moneta.txn.TxnStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {
  private final AlertRepository alertRepository;
  private final BudgetRepository budgetRepository;
  private final BudgetCalculator budgetCalculator;

  public AlertService(
    AlertRepository alertRepository,
    BudgetRepository budgetRepository,
    BudgetCalculator budgetCalculator
  ) {
    this.alertRepository = alertRepository;
    this.budgetRepository = budgetRepository;
    this.budgetCalculator = budgetCalculator;
  }

  public List<Alert> list(Long userId, String monthRef) {
    if (monthRef != null && !monthRef.isBlank()) {
      MonthRefValidator.validate(monthRef);
      return alertRepository.findAllByUserIdAndMonthRef(userId, monthRef);
    }
    return alertRepository.findAllByUserId(userId);
  }

  @Transactional
  public Alert markRead(Long userId, Long alertId, boolean isRead) {
    Alert alert = alertRepository.findByIdAndUserId(alertId, userId)
      .orElseThrow(() -> new IllegalArgumentException("alerta não encontrado"));
    alert.setRead(isRead);
    return alertRepository.save(alert);
  }

  @Transactional
  public void evaluateBudget(Budget budget) {
    long consumption = budgetCalculator.calculateConsumption(
      budget.getUser().getId(),
      budget.getMonthRef(),
      budget.getCategoryId(),
      budget.getSubcategoryId()
    );
    double percent = budget.getLimitCents() == 0 ? 0.0 : (double) consumption / (double) budget.getLimitCents();
    createAlertIfNeeded(budget, percent, AlertType.BUDGET_80, 0.8);
    createAlertIfNeeded(budget, percent, AlertType.BUDGET_100, 1.0);
  }

  @Transactional
  public void evaluateBudgetsForTxn(Txn txn) {
    if (txn.getStatus() != TxnStatus.POSTED || txn.getDirection() != TxnDirection.OUT || !txn.isActive()) {
      return;
    }
    List<Budget> budgets = budgetRepository.findAllByUserIdAndMonthRef(txn.getUser().getId(), txn.getMonthRef());
    if (budgets.isEmpty()) {
      return;
    }
    for (Budget budget : budgets) {
      if (!matchesBudget(txn, budget)) {
        continue;
      }
      evaluateBudget(budget);
    }
  }

  private boolean matchesBudget(Txn txn, Budget budget) {
    if (budget.getSubcategoryId() != null) {
      return budget.getSubcategoryId().equals(txn.getSubcategoryId());
    }
    if (budget.getCategoryId() != null) {
      return budget.getCategoryId().equals(txn.getCategoryId());
    }
    return false;
  }

  private void createAlertIfNeeded(Budget budget, double percent, AlertType type, double threshold) {
    if (percent < threshold) {
      return;
    }
    Long userId = budget.getUser().getId();
    if (alertRepository.existsByUserIdAndBudgetIdAndMonthRefAndType(userId, budget.getId(), budget.getMonthRef(), type)) {
      return;
    }
    Alert alert = new Alert();
    alert.setUser(budget.getUser());
    alert.setType(type);
    alert.setMonthRef(budget.getMonthRef());
    alert.setBudgetId(budget.getId());
    alert.setMessage(buildMessage(budget, type));
    alert.setTriggeredAt(OffsetDateTime.now());
    try {
      alertRepository.save(alert);
    } catch (org.springframework.dao.DataIntegrityViolationException e) {
      // Duplicate alert created by concurrent request; ignore
    }
  }

  private String buildMessage(Budget budget, AlertType type) {
    String base = type == AlertType.BUDGET_80 ? "80%" : "100%";
    String target = budget.getSubcategoryId() != null ? "subcategoria" : "categoria";
    return String.format(
      Locale.ROOT,
      "Orçamento %s atingiu %s no mês %s",
      target,
      base,
      budget.getMonthRef()
    );
  }
}
