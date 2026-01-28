package com.moneta.dashboard;

import com.moneta.alert.Alert;
import com.moneta.alert.AlertRepository;
import com.moneta.alert.AlertType;
import com.moneta.budget.Budget;
import com.moneta.budget.BudgetCalculator;
import com.moneta.budget.BudgetRepository;
import com.moneta.category.Category;
import com.moneta.category.CategoryRepository;
import com.moneta.common.MonthRefValidator;
import com.moneta.dashboard.DashboardDtos.AlertSummary;
import com.moneta.dashboard.DashboardDtos.BudgetStatus;
import com.moneta.dashboard.DashboardDtos.CategorySpend;
import com.moneta.dashboard.DashboardDtos.MonthlyResponse;
import com.moneta.txn.TxnRepository;
import com.moneta.txn.TxnRepository.CategoryExpenseProjection;
import com.moneta.txn.TxnRepository.MonthlyTotalsProjection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
  private final TxnRepository txnRepository;
  private final CategoryRepository categoryRepository;
  private final BudgetRepository budgetRepository;
  private final BudgetCalculator budgetCalculator;
  private final AlertRepository alertRepository;

  public DashboardService(
    TxnRepository txnRepository,
    CategoryRepository categoryRepository,
    BudgetRepository budgetRepository,
    BudgetCalculator budgetCalculator,
    AlertRepository alertRepository
  ) {
    this.txnRepository = txnRepository;
    this.categoryRepository = categoryRepository;
    this.budgetRepository = budgetRepository;
    this.budgetCalculator = budgetCalculator;
    this.alertRepository = alertRepository;
  }

  public MonthlyResponse getMonthly(Long userId, String monthRef) {
    MonthRefValidator.validate(monthRef);

    MonthlyTotalsProjection totals = txnRepository.findMonthlyTotals(userId, monthRef);
    long income = totals == null ? 0L : totals.getIncomeCents();
    long expense = totals == null ? 0L : totals.getExpenseCents();
    long net = income - expense;

    List<CategoryExpenseProjection> expenseRows = txnRepository.findCategoryExpenses(userId, monthRef);
    Map<Long, String> categoryNames = categoryRepository.findAllByUserIdAndIsActiveTrue(userId).stream()
      .collect(Collectors.toMap(Category::getId, Category::getName));
    List<CategorySpend> byCategory = expenseRows.stream()
      .map(row -> new CategorySpend(
        row.getCategoryId(),
        categoryNames.get(row.getCategoryId()),
        row.getExpenseCents()
      ))
      .toList();

    List<Budget> budgets = budgetRepository.findAllByUserIdAndMonthRef(userId, monthRef);
    List<Alert> alerts = alertRepository.findAllByUserIdAndMonthRef(userId, monthRef);
    Map<Long, List<AlertType>> alertMap = alerts.stream()
      .collect(Collectors.groupingBy(Alert::getBudgetId, Collectors.mapping(Alert::getType, Collectors.toList())));

    List<BudgetStatus> budgetStatuses = new ArrayList<>();
    for (Budget budget : budgets) {
      long consumption = budgetCalculator.calculateConsumption(
        userId,
        budget.getMonthRef(),
        budget.getCategoryId(),
        budget.getSubcategoryId()
      );
      double percent = budget.getLimitCents() == 0 ? 0.0 : (double) consumption / (double) budget.getLimitCents();
      List<AlertType> types = alertMap.getOrDefault(budget.getId(), List.of());
      budgetStatuses.add(new BudgetStatus(
        budget.getId(),
        budget.getCategoryId(),
        budget.getSubcategoryId(),
        budget.getLimitCents(),
        consumption,
        percent,
        types.contains(AlertType.BUDGET_80),
        types.contains(AlertType.BUDGET_100)
      ));
    }

    List<AlertSummary> alertSummaries = alerts.stream()
      .map(alert -> new AlertSummary(
        alert.getId(),
        alert.getType(),
        alert.getMessage(),
        alert.isRead(),
        alert.getTriggeredAt()
      ))
      .toList();

    return new MonthlyResponse(monthRef, income, expense, net, byCategory, budgetStatuses, alertSummaries);
  }
}
