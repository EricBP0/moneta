package com.moneta.budget;

import com.moneta.budget.BudgetDtos.BudgetRequest;
import com.moneta.budget.BudgetDtos.BudgetResponse;
import com.moneta.config.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {
  private final BudgetService budgetService;

  public BudgetController(BudgetService budgetService) {
    this.budgetService = budgetService;
  }

  @GetMapping
  public List<BudgetResponse> list(
    @AuthenticationPrincipal UserPrincipal principal,
    @RequestParam(required = false) String month
  ) {
    return budgetService.list(principal.getId(), month).stream()
      .map(this::toResponse)
      .toList();
  }

  @PostMapping
  public BudgetResponse create(
    @AuthenticationPrincipal UserPrincipal principal,
    @Valid @RequestBody BudgetRequest request
  ) {
    return toResponse(budgetService.create(principal.getId(), request));
  }

  @DeleteMapping("/{id}")
  public void delete(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id
  ) {
    budgetService.delete(principal.getId(), id);
  }

  private BudgetResponse toResponse(Budget budget) {
    return new BudgetResponse(
      budget.getId(),
      budget.getMonthRef(),
      budget.getCategoryId(),
      budget.getSubcategoryId(),
      budget.getLimitCents()
    );
  }
}
