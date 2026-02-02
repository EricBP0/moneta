package com.moneta.card;

import java.time.LocalDate;
import java.time.YearMonth;

public class BillingCycleCalculator {
  public record BillingCycle(
    int year,
    int month,
    LocalDate closingDate,
    LocalDate dueDate,
    LocalDate startDate,
    LocalDate endDateExclusive
  ) {}

  public static BillingCycle forOccurredAt(LocalDate occurredAt, int closingDay, int dueDay) {
    YearMonth closingMonth = YearMonth.from(occurredAt);
    LocalDate closingDateInMonth = resolveClosingDate(closingMonth, closingDay);
    if (occurredAt.getDayOfMonth() > closingDateInMonth.getDayOfMonth()) {
      closingMonth = closingMonth.plusMonths(1);
    }
    return forMonth(closingMonth.getYear(), closingMonth.getMonthValue(), closingDay, dueDay);
  }

  public static BillingCycle forMonth(int year, int month, int closingDay, int dueDay) {
    YearMonth closingMonth = YearMonth.of(year, month);
    LocalDate closingDate = resolveClosingDate(closingMonth, closingDay);
    LocalDate previousClosingDate = resolveClosingDate(closingMonth.minusMonths(1), closingDay);
    LocalDate startDate = previousClosingDate.plusDays(1);
    LocalDate endDateExclusive = closingDate.plusDays(1);
    LocalDate dueDate = resolveClosingDate(closingMonth.plusMonths(1), dueDay);
    return new BillingCycle(
      closingDate.getYear(),
      closingDate.getMonthValue(),
      closingDate,
      dueDate,
      startDate,
      endDateExclusive
    );
  }

  private static LocalDate resolveClosingDate(YearMonth month, int day) {
    int safeDay = Math.min(day, month.lengthOfMonth());
    return month.atDay(safeDay);
  }
}
