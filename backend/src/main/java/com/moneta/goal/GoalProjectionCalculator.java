package com.moneta.goal;

import com.moneta.goal.GoalDtos.GoalProjectionResponse;
import com.moneta.goal.GoalDtos.GoalProjectionScheduleItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GoalProjectionCalculator {
  private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

  public GoalProjectionResponse calculate(Goal goal, long savedSoFarCents, YearMonth asOfMonth) {
    YearMonth targetMonth = YearMonth.from(goal.getTargetDate());
    if (asOfMonth.isAfter(targetMonth)) {
      return new GoalProjectionResponse(
        savedSoFarCents,
        goal.getTargetAmountCents(),
        0,
        0L,
        asOfMonth.format(MONTH_FORMATTER),
        List.of()
      );
    }

    int monthsRemaining = (int) (monthsBetween(asOfMonth, targetMonth) + 1);
    if (savedSoFarCents >= goal.getTargetAmountCents()) {
      return new GoalProjectionResponse(
        savedSoFarCents,
        goal.getTargetAmountCents(),
        monthsRemaining,
        0L,
        asOfMonth.format(MONTH_FORMATTER),
        List.of()
      );
    }

    double monthlyRate = goal.getMonthlyRateBps() == null ? 0.0 : goal.getMonthlyRateBps() / 10000.0;
    long neededMonthly = calculateNeededMonthly(goal.getTargetAmountCents(), savedSoFarCents, monthsRemaining, monthlyRate);
    List<GoalProjectionScheduleItem> schedule = buildSchedule(savedSoFarCents, neededMonthly, monthsRemaining, monthlyRate, asOfMonth);
    String estimatedCompletion = estimateCompletionMonth(schedule, goal.getTargetAmountCents(), targetMonth, asOfMonth);

    return new GoalProjectionResponse(
      savedSoFarCents,
      goal.getTargetAmountCents(),
      monthsRemaining,
      neededMonthly,
      estimatedCompletion,
      schedule
    );
  }

  public long calculateExpectedSaved(Goal goal, YearMonth asOfMonth) {
    YearMonth startMonth = YearMonth.from(goal.getStartDate());
    YearMonth targetMonth = YearMonth.from(goal.getTargetDate());
    long totalMonths = monthsBetween(startMonth, targetMonth) + 1;
    if (asOfMonth.isBefore(startMonth)) {
      return 0L;
    }
    long elapsedMonths = asOfMonth.isAfter(targetMonth)
      ? totalMonths
      : monthsBetween(startMonth, asOfMonth) + 1;
    BigDecimal ratio = BigDecimal.valueOf(elapsedMonths)
      .divide(BigDecimal.valueOf(totalMonths), 6, RoundingMode.HALF_UP);
    return BigDecimal.valueOf(goal.getTargetAmountCents())
      .multiply(ratio)
      .setScale(0, RoundingMode.FLOOR)
      .longValue();
  }

  public LocalDate endOfMonth(YearMonth month) {
    return month.atEndOfMonth();
  }

  private long calculateNeededMonthly(long target, long saved, int monthsRemaining, double monthlyRate) {
    if (monthsRemaining <= 0) {
      return 0L;
    }
    if (monthlyRate <= 0) {
      return divideCeil(target - saved, monthsRemaining);
    }
    double pow = Math.pow(1 + monthlyRate, monthsRemaining);
    double numerator = target - (saved * pow);
    double denominator = (pow - 1) / monthlyRate;
    double payment = numerator <= 0 ? 0 : numerator / denominator;
    return (long) Math.ceil(payment);
  }

  private List<GoalProjectionScheduleItem> buildSchedule(
    long savedSoFar,
    long neededMonthly,
    int monthsRemaining,
    double monthlyRate,
    YearMonth asOfMonth
  ) {
    if (monthsRemaining <= 0) {
      return List.of();
    }
    List<GoalProjectionScheduleItem> schedule = new ArrayList<>();
    for (int i = 1; i <= monthsRemaining; i += 1) {
      double projected = monthlyRate <= 0
        ? savedSoFar + neededMonthly * i
        : savedSoFar * Math.pow(1 + monthlyRate, i)
          + neededMonthly * ((Math.pow(1 + monthlyRate, i) - 1) / monthlyRate);
      schedule.add(new GoalProjectionScheduleItem(
        asOfMonth.plusMonths(i - 1).format(MONTH_FORMATTER),
        Math.round(projected),
        neededMonthly
      ));
    }
    return schedule;
  }

  private String estimateCompletionMonth(
    List<GoalProjectionScheduleItem> schedule,
    long targetAmountCents,
    YearMonth targetMonth,
    YearMonth asOfMonth
  ) {
    for (GoalProjectionScheduleItem item : schedule) {
      if (item.savedProjectedCents() >= targetAmountCents) {
        return item.month();
      }
    }
    if (!schedule.isEmpty()) {
      return targetMonth.format(MONTH_FORMATTER);
    }
    return asOfMonth.format(MONTH_FORMATTER);
  }

  private long monthsBetween(YearMonth start, YearMonth end) {
    return (end.getYear() - start.getYear()) * 12L + (end.getMonthValue() - start.getMonthValue());
  }

  private long divideCeil(long numerator, long divisor) {
    if (divisor <= 0) {
      return 0L;
    }
    return (numerator + divisor - 1) / divisor;
  }
}
