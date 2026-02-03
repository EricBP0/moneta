package com.moneta.card;

import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class BillingCycleService {

  /**
   * Calculates the billing cycle for a given date and card closing/due days.
   * 
   * @param occurredAt The transaction date
   * @param closingDay Day of month when the billing cycle closes (1-31)
   * @param dueDay Day of month when payment is due (1-31)
   * @return BillingCycle information
   */
  public CardInvoiceDtos.BillingCycle calculateBillingCycle(LocalDate occurredAt, int closingDay, int dueDay) {
    LocalDate closingDate;
    
    // Determine which billing cycle this transaction belongs to
    int dayOfMonth = occurredAt.getDayOfMonth();
    
    if (dayOfMonth <= closingDay) {
      // Transaction is in the current month's cycle
      closingDate = getAdjustedDate(occurredAt.getYear(), occurredAt.getMonthValue(), closingDay);
    } else {
      // Transaction is in the next month's cycle
      LocalDate nextMonth = occurredAt.plusMonths(1);
      closingDate = getAdjustedDate(nextMonth.getYear(), nextMonth.getMonthValue(), closingDay);
    }
    
    // Due date is in the month following the closing date
    LocalDate dueMonth = closingDate.plusMonths(1);
    LocalDate dueDate = getAdjustedDate(dueMonth.getYear(), dueMonth.getMonthValue(), dueDay);
    
    // Start date is the day after the previous cycle's closing
    LocalDate previousClosing = closingDate.minusMonths(1);
    LocalDate startDate = previousClosing.plusDays(1);
    
    // End date is the day after this cycle's closing (exclusive)
    LocalDate endDate = closingDate.plusDays(1);
    
    return new CardInvoiceDtos.BillingCycle(
      closingDate.getYear(),
      closingDate.getMonthValue(),
      closingDate,
      dueDate,
      startDate,
      endDate
    );
  }

  /**
   * Calculates billing cycle for a specific year and month.
   * This is used when querying invoices by year/month.
   * 
   * @param year The year
   * @param month The month (1-12)
   * @param closingDay Day of month when the billing cycle closes (1-31)
   * @param dueDay Day of month when payment is due (1-31)
   * @return BillingCycle information
   */
  public CardInvoiceDtos.BillingCycle calculateBillingCycleForMonth(int year, int month, int closingDay, int dueDay) {
    LocalDate closingDate = getAdjustedDate(year, month, closingDay);
    
    // Due date is in the month following the closing date
    LocalDate dueMonth = closingDate.plusMonths(1);
    LocalDate dueDate = getAdjustedDate(dueMonth.getYear(), dueMonth.getMonthValue(), dueDay);
    
    // Start date is the day after the previous cycle's closing
    LocalDate previousClosing = closingDate.minusMonths(1);
    LocalDate startDate = previousClosing.plusDays(1);
    
    // End date is the day after this cycle's closing (exclusive)
    LocalDate endDate = closingDate.plusDays(1);
    
    return new CardInvoiceDtos.BillingCycle(
      year,
      month,
      closingDate,
      dueDate,
      startDate,
      endDate
    );
  }

  /**
   * Gets a date adjusted for months that don't have the specified day.
   * For example, if closingDay is 31 but the month only has 30 days,
   * returns the last day of that month.
   */
  private LocalDate getAdjustedDate(int year, int month, int dayOfMonth) {
    LocalDate firstOfMonth = LocalDate.of(year, month, 1);
    int lastDayOfMonth = firstOfMonth.lengthOfMonth();
    int adjustedDay = Math.min(dayOfMonth, lastDayOfMonth);
    return LocalDate.of(year, month, adjustedDay);
  }
}
