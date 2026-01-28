package com.moneta.goal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class GoalProjectionCalculatorTest {
  private final GoalProjectionCalculator calculator = new GoalProjectionCalculator();

  @Test
  void calculatesProjectionWithoutInterest() {
    Goal goal = new Goal();
    goal.setTargetAmountCents(1200L);
    goal.setStartDate(LocalDate.of(2024, 1, 1));
    goal.setTargetDate(LocalDate.of(2024, 3, 1));
    goal.setMonthlyRateBps(0);

    var projection = calculator.calculate(goal, 0L, YearMonth.of(2024, 1));

    assertThat(projection.monthsRemaining()).isEqualTo(3);
    assertThat(projection.neededMonthlyCents()).isEqualTo(400L);
  }

  @Test
  void calculatesProjectionWithInterest() {
    Goal goal = new Goal();
    goal.setTargetAmountCents(1200L);
    goal.setStartDate(LocalDate.of(2024, 1, 1));
    goal.setTargetDate(LocalDate.of(2024, 3, 1));
    goal.setMonthlyRateBps(200); // 2%

    var projection = calculator.calculate(goal, 200L, YearMonth.of(2024, 1));

    assertThat(projection.monthsRemaining()).isEqualTo(3);
    assertThat(projection.neededMonthlyCents()).isLessThan(400L);
  }
}
