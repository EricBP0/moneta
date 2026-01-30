package com.moneta.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.moneta.alert.Alert;
import com.moneta.alert.AlertRepository;
import com.moneta.alert.AlertType;
import com.moneta.budget.Budget;
import com.moneta.budget.BudgetCalculator;
import com.moneta.budget.BudgetRepository;
import com.moneta.category.Category;
import com.moneta.category.CategoryRepository;
import com.moneta.goal.Goal;
import com.moneta.goal.GoalContributionRepository;
import com.moneta.goal.GoalProjectionCalculator;
import com.moneta.goal.GoalRepository;
import com.moneta.txn.TxnRepository;
import com.moneta.txn.TxnRepository.CategoryExpenseProjection;
import com.moneta.txn.TxnRepository.MonthlyTotalsProjection;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
  @Mock
  private TxnRepository txnRepository;

  @Mock
  private CategoryRepository categoryRepository;

  @Mock
  private BudgetRepository budgetRepository;

  @Mock
  private BudgetCalculator budgetCalculator;

  @Mock
  private AlertRepository alertRepository;

  @Mock
  private GoalRepository goalRepository;

  @Mock
  private GoalContributionRepository goalContributionRepository;

  @Mock
  private GoalProjectionCalculator goalProjectionCalculator;

  private DashboardService dashboardService;

  @BeforeEach
  void setup() {
    dashboardService = new DashboardService(
      txnRepository,
      categoryRepository,
      budgetRepository,
      budgetCalculator,
      alertRepository,
      goalRepository,
      goalContributionRepository,
      goalProjectionCalculator
    );
  }

  @Test
  void aggregatesMonthlyDataAndAlerts() {
    when(txnRepository.findMonthlyTotals(1L, "2024-08")).thenReturn(new MonthlyTotalsProjection() {
      @Override
      public Long getIncomeCents() {
        return 10000L;
      }

      @Override
      public Long getExpenseCents() {
        return 4000L;
      }
    });

    when(txnRepository.findCategoryExpenses(1L, "2024-08")).thenReturn(List.of(new CategoryExpenseProjection() {
      @Override
      public Long getCategoryId() {
        return 10L;
      }

      @Override
      public Long getExpenseCents() {
        return 4000L;
      }
    }));

    Category category = org.mockito.Mockito.mock(Category.class);
    when(category.getId()).thenReturn(10L);
    when(category.getName()).thenReturn("Mercado");
    when(categoryRepository.findAllByUserIdAndIsActiveTrue(1L)).thenReturn(List.of(category));

    Budget budget = org.mockito.Mockito.mock(Budget.class);
    when(budget.getId()).thenReturn(55L);
    when(budget.getCategoryId()).thenReturn(10L);
    when(budget.getSubcategoryId()).thenReturn(null);
    when(budget.getMonthRef()).thenReturn("2024-08");
    when(budget.getLimitCents()).thenReturn(5000L);
    when(budgetRepository.findAllByUserIdAndMonthRef(1L, "2024-08")).thenReturn(List.of(budget));
    when(budgetCalculator.calculateConsumption(1L, "2024-08", 10L, null)).thenReturn(4000L);

    Alert alert80 = new Alert();
    alert80.setBudgetId(55L);
    alert80.setType(AlertType.BUDGET_80);
    alert80.setMessage("Alerta 80%");
    alert80.setRead(false);
    when(alertRepository.findAllByUserIdAndMonthRef(1L, "2024-08")).thenReturn(List.of(alert80));

    Goal goal = org.mockito.Mockito.mock(Goal.class);
    when(goal.getId()).thenReturn(90L);
    when(goal.getName()).thenReturn("Reserva");
    when(goal.getTargetAmountCents()).thenReturn(10000L);
    when(goal.getTargetDate()).thenReturn(LocalDate.of(2024, 12, 1));
    when(goal.getStatus()).thenReturn(com.moneta.goal.GoalStatus.ACTIVE);
    when(goalRepository.findAllByUserId(1L)).thenReturn(List.of(goal));
    when(goalProjectionCalculator.endOfMonth(YearMonth.of(2024, 8))).thenReturn(LocalDate.of(2024, 8, 31));
    when(goalContributionRepository.sumByGoalIdUpTo(1L, 90L, LocalDate.of(2024, 8, 31))).thenReturn(4000L);
    when(goalProjectionCalculator.calculate(goal, 4000L, YearMonth.of(2024, 8)))
      .thenReturn(new com.moneta.goal.GoalDtos.GoalProjectionResponse(
        4000L,
        10000L,
        4,
        1500L,
        "2024-12",
        List.of()
      ));

    var response = dashboardService.getMonthly(1L, "2024-08");

    assertThat(response.incomeCents()).isEqualTo(10000L);
    assertThat(response.expenseCents()).isEqualTo(4000L);
    assertThat(response.netCents()).isEqualTo(6000L);
    assertThat(response.byCategory()).hasSize(1);
    assertThat(response.byCategory().get(0).categoryName()).isEqualTo("Mercado");
    assertThat(response.budgetStatus()).hasSize(1);
    assertThat(response.budgetStatus().get(0).triggered80()).isTrue();
    assertThat(response.budgetStatus().get(0).triggered100()).isFalse();
    assertThat(response.alerts()).hasSize(1);
    assertThat(response.goalsSummary()).hasSize(1);
  }

  @Test
  void handlesAlertsWithNullBudgetIdGracefully() {
    when(txnRepository.findMonthlyTotals(1L, "2024-08")).thenReturn(new MonthlyTotalsProjection() {
      @Override
      public Long getIncomeCents() {
        return 10000L;
      }

      @Override
      public Long getExpenseCents() {
        return 4000L;
      }
    });

    when(txnRepository.findCategoryExpenses(1L, "2024-08")).thenReturn(List.of());
    when(categoryRepository.findAllByUserIdAndIsActiveTrue(1L)).thenReturn(List.of());

    Budget budget = org.mockito.Mockito.mock(Budget.class);
    when(budget.getId()).thenReturn(55L);
    when(budget.getCategoryId()).thenReturn(10L);
    when(budget.getSubcategoryId()).thenReturn(null);
    when(budget.getMonthRef()).thenReturn("2024-08");
    when(budget.getLimitCents()).thenReturn(5000L);
    when(budgetRepository.findAllByUserIdAndMonthRef(1L, "2024-08")).thenReturn(List.of(budget));
    when(budgetCalculator.calculateConsumption(1L, "2024-08", 10L, null)).thenReturn(4000L);

    // Create alerts with null budgetId (e.g., goal-related alerts)
    Alert alertWithNullBudgetId = new Alert();
    alertWithNullBudgetId.setBudgetId(null);
    alertWithNullBudgetId.setType(AlertType.GOAL_BEHIND);
    alertWithNullBudgetId.setMessage("Goal behind schedule");
    alertWithNullBudgetId.setRead(false);

    Alert alertWithBudgetId = new Alert();
    alertWithBudgetId.setBudgetId(55L);
    alertWithBudgetId.setType(AlertType.BUDGET_80);
    alertWithBudgetId.setMessage("Budget 80%");
    alertWithBudgetId.setRead(false);

    when(alertRepository.findAllByUserIdAndMonthRef(1L, "2024-08"))
      .thenReturn(List.of(alertWithNullBudgetId, alertWithBudgetId));

    when(goalRepository.findAllByUserId(1L)).thenReturn(List.of());

    // Should not throw NPE
    var response = dashboardService.getMonthly(1L, "2024-08");

    // Verify response is valid
    assertThat(response).isNotNull();
    assertThat(response.alerts()).hasSize(2);
    assertThat(response.budgetStatus()).hasSize(1);
    assertThat(response.budgetStatus().get(0).triggered80()).isTrue();
  }
}
