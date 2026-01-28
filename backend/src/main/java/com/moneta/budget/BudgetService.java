package com.moneta.budget;

import com.moneta.alert.AlertService;
import com.moneta.auth.User;
import com.moneta.auth.UserRepository;
import com.moneta.category.CategoryRepository;
import com.moneta.common.MonthRefValidator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetService {
  private final BudgetRepository budgetRepository;
  private final UserRepository userRepository;
  private final CategoryRepository categoryRepository;
  private final BudgetCalculator budgetCalculator;
  private final AlertService alertService;

  public BudgetService(
    BudgetRepository budgetRepository,
    UserRepository userRepository,
    CategoryRepository categoryRepository,
    BudgetCalculator budgetCalculator,
    AlertService alertService
  ) {
    this.budgetRepository = budgetRepository;
    this.userRepository = userRepository;
    this.categoryRepository = categoryRepository;
    this.budgetCalculator = budgetCalculator;
    this.alertService = alertService;
  }

  @Transactional
  public Budget create(Long userId, BudgetDtos.BudgetRequest request) {
    MonthRefValidator.validate(request.monthRef());
    validateTarget(request.categoryId(), request.subcategoryId());
    validateCategory(userId, request.categoryId());
    ensureUnique(userId, request.monthRef(), request.categoryId(), request.subcategoryId());

    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("usuário não encontrado"));

    Budget budget = new Budget();
    budget.setUser(user);
    budget.setMonthRef(request.monthRef());
    budget.setCategoryId(request.categoryId());
    budget.setSubcategoryId(request.subcategoryId());
    budget.setLimitCents(request.limitCents());
    Budget saved = budgetRepository.save(budget);
    alertService.evaluateBudget(saved);
    return saved;
  }

  public List<Budget> list(Long userId, String monthRef) {
    if (monthRef != null && !monthRef.isBlank()) {
      MonthRefValidator.validate(monthRef);
      return budgetRepository.findAllByUserIdAndMonthRef(userId, monthRef);
    }
    return budgetRepository.findAllByUserId(userId);
  }

  @Transactional
  public void delete(Long userId, Long id) {
    Budget budget = budgetRepository.findByIdAndUserId(id, userId)
      .orElseThrow(() -> new IllegalArgumentException("orçamento não encontrado"));
    budgetRepository.delete(budget);
  }

  public long calculateConsumption(Budget budget) {
    return budgetCalculator.calculateConsumption(
      budget.getUser().getId(),
      budget.getMonthRef(),
      budget.getCategoryId(),
      budget.getSubcategoryId()
    );
  }

  public double calculatePercent(long consumptionCents, long limitCents) {
    if (limitCents <= 0) {
      return 0.0;
    }
    return (double) consumptionCents / (double) limitCents;
  }

  private void ensureUnique(Long userId, String monthRef, Long categoryId, Long subcategoryId) {
    budgetRepository.findExisting(userId, monthRef, categoryId, subcategoryId).ifPresent(existing -> {
      throw new IllegalArgumentException("orçamento já existe para este mês e categoria");
    });
  }

  private void validateTarget(Long categoryId, Long subcategoryId) {
    if (categoryId == null && subcategoryId == null) {
      throw new IllegalArgumentException("categoria ou subcategoria é obrigatória");
    }
    if (categoryId != null && subcategoryId != null) {
      throw new IllegalArgumentException("orçamento deve ser para categoria ou subcategoria, não ambos");
    }
  }

  private void validateCategory(Long userId, Long categoryId) {
    if (categoryId == null) {
      return;
    }
    categoryRepository.findByIdAndUserId(categoryId, userId)
      .orElseThrow(() -> new IllegalArgumentException("categoria não encontrada"));
  }
}
