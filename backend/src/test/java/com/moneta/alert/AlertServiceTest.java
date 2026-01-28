package com.moneta.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moneta.auth.User;
import com.moneta.budget.Budget;
import com.moneta.budget.BudgetCalculator;
import com.moneta.budget.BudgetRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {
  @Mock
  private AlertRepository alertRepository;

  @Mock
  private BudgetRepository budgetRepository;

  @Mock
  private BudgetCalculator budgetCalculator;

  private AlertService alertService;

  @BeforeEach
  void setup() {
    alertService = new AlertService(alertRepository, budgetRepository, budgetCalculator);
  }

  @Test
  void evaluateBudgetCreates80And100Alerts() {
    Budget budget = new Budget();
    User user = org.mockito.Mockito.mock(User.class);
    when(user.getId()).thenReturn(1L);
    budget.setUser(user);
    budget.setMonthRef("2024-08");
    budget.setLimitCents(1000L);

    when(budgetCalculator.calculateConsumption(1L, "2024-08", null, null)).thenReturn(1200L);
    when(alertRepository.existsByUserIdAndBudgetIdAndMonthRefAndType(1L, budget.getId(), "2024-08", AlertType.BUDGET_80))
      .thenReturn(false);
    when(alertRepository.existsByUserIdAndBudgetIdAndMonthRefAndType(1L, budget.getId(), "2024-08", AlertType.BUDGET_100))
      .thenReturn(false);

    alertService.evaluateBudget(budget);

    verify(alertRepository, times(2)).save(any(Alert.class));
  }

  @Test
  void evaluateBudgetAvoidsDuplicateAlerts() {
    Budget budget = new Budget();
    User user = org.mockito.Mockito.mock(User.class);
    when(user.getId()).thenReturn(1L);
    budget.setUser(user);
    budget.setMonthRef("2024-08");
    budget.setLimitCents(1000L);

    when(budgetCalculator.calculateConsumption(1L, "2024-08", null, null)).thenReturn(900L);
    when(alertRepository.existsByUserIdAndBudgetIdAndMonthRefAndType(1L, budget.getId(), "2024-08", AlertType.BUDGET_80))
      .thenReturn(true);

    alertService.evaluateBudget(budget);

    verify(alertRepository, never()).save(any(Alert.class));
  }

  @Test
  void markReadUpdatesAlert() {
    Alert alert = new Alert();
    when(alertRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(alert));
    when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Alert updated = alertService.markRead(1L, 10L, true);

    assertThat(updated.isRead()).isTrue();
  }
}
