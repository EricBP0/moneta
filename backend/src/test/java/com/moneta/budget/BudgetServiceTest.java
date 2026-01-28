package com.moneta.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.moneta.alert.AlertService;
import com.moneta.auth.User;
import com.moneta.auth.UserRepository;
import com.moneta.category.CategoryRepository;
import com.moneta.txn.TxnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {
  @Mock
  private BudgetRepository budgetRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private CategoryRepository categoryRepository;

  @Mock
  private TxnRepository txnRepository;

  @Mock
  private AlertService alertService;

  private BudgetCalculator budgetCalculator;
  private BudgetService budgetService;

  @BeforeEach
  void setup() {
    budgetCalculator = new BudgetCalculator(txnRepository);
    budgetService = new BudgetService(
      budgetRepository,
      userRepository,
      categoryRepository,
      budgetCalculator,
      alertService
    );
  }

  @Test
  void createValidatesMonthRef() {
    BudgetDtos.BudgetRequest request = new BudgetDtos.BudgetRequest("2024-13", 1L, null, 1000L);
    assertThatThrownBy(() -> budgetService.create(1L, request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("mês inválido");
  }

  @Test
  void createEnforcesUniqueness() {
    when(categoryRepository.findByIdAndUserId(1L, 1L)).thenReturn(java.util.Optional.of(new com.moneta.category.Category()));
    Budget existing = new Budget();
    when(budgetRepository.findExisting(1L, "2024-08", 1L, null)).thenReturn(java.util.Optional.of(existing));
    BudgetDtos.BudgetRequest request = new BudgetDtos.BudgetRequest("2024-08", 1L, null, 1000L);
    assertThatThrownBy(() -> budgetService.create(1L, request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("orçamento já existe");
  }

  @Test
  void calculateConsumptionUsesCategoryAndSubcategory() {
    User user = org.mockito.Mockito.mock(User.class);
    when(user.getId()).thenReturn(1L);
    Budget categoryBudget = new Budget();
    categoryBudget.setUser(user);
    categoryBudget.setMonthRef("2024-08");
    categoryBudget.setCategoryId(10L);
    categoryBudget.setLimitCents(1000L);
    when(txnRepository.sumPostedOutByUserAndMonthAndCategory(1L, "2024-08", 10L, null)).thenReturn(500L);

    long categoryConsumption = budgetService.calculateConsumption(categoryBudget);
    assertThat(categoryConsumption).isEqualTo(500L);

    Budget subcategoryBudget = new Budget();
    subcategoryBudget.setUser(user);
    subcategoryBudget.setMonthRef("2024-08");
    subcategoryBudget.setSubcategoryId(20L);
    subcategoryBudget.setLimitCents(1000L);
    when(txnRepository.sumPostedOutByUserAndMonthAndCategory(1L, "2024-08", null, 20L)).thenReturn(300L);

    long subcategoryConsumption = budgetService.calculateConsumption(subcategoryBudget);
    assertThat(subcategoryConsumption).isEqualTo(300L);
  }
}
