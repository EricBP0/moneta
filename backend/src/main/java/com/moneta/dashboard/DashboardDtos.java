package com.moneta.dashboard;

import com.moneta.alert.AlertType;
import java.time.OffsetDateTime;
import java.util.List;

public class DashboardDtos {
  public record MonthlyResponse(
    String month,
    long incomeCents,
    long expenseCents,
    long netCents,
    List<CategorySpend> byCategory,
    List<BudgetStatus> budgetStatus,
    List<AlertSummary> alerts
  ) {}

  public record CategorySpend(
    Long categoryId,
    String categoryName,
    long expenseCents
  ) {}

  public record BudgetStatus(
    Long budgetId,
    Long categoryId,
    Long subcategoryId,
    long limitCents,
    long consumptionCents,
    double percent,
    boolean triggered80,
    boolean triggered100
  ) {}

  public record AlertSummary(
    Long id,
    AlertType type,
    String message,
    boolean isRead,
    OffsetDateTime triggeredAt
  ) {}
}
