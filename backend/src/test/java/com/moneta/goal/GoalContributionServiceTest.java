package com.moneta.goal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moneta.alert.AlertService;
import com.moneta.auth.User;
import com.moneta.auth.UserRepository;
import com.moneta.goal.GoalDtos.GoalProjectionResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoalContributionServiceTest {
  @Mock
  private GoalRepository goalRepository;

  @Mock
  private GoalContributionRepository contributionRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private AlertService alertService;

  @Mock
  private GoalProjectionCalculator projectionCalculator;

  private GoalContributionService service;

  @BeforeEach
  void setup() {
    service = new GoalContributionService(
      goalRepository,
      contributionRepository,
      userRepository,
      alertService,
      projectionCalculator
    );
  }

  @Test
  void addContributionCalculatesProjection() {
    Goal goal = org.mockito.Mockito.mock(Goal.class);
    User user = new User();

    when(goalRepository.findByIdAndUserId(10L, 1L)).thenReturn(java.util.Optional.of(goal));
    when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));

    GoalContribution saved = new GoalContribution();
    saved.setGoal(goal);
    saved.setUser(user);
    saved.setContributedAt(LocalDate.of(2024, 8, 1));
    saved.setAmountCents(200L);
    when(contributionRepository.save(org.mockito.ArgumentMatchers.any(GoalContribution.class))).thenReturn(saved);

    when(projectionCalculator.endOfMonth(YearMonth.of(2024, 8))).thenReturn(LocalDate.of(2024, 8, 31));
    when(contributionRepository.sumByGoalIdUpTo(1L, 10L, LocalDate.of(2024, 8, 31)))
      .thenReturn(200L);

    GoalProjectionResponse projection = new GoalProjectionResponse(200L, 1000L, 4, 200L, "2024-11", List.of());
    when(projectionCalculator.calculate(goal, 200L, YearMonth.of(2024, 8))).thenReturn(projection);

    GoalDtos.GoalContributionRequest request = new GoalDtos.GoalContributionRequest("2024-08-01", 200L, "aporte");
    var result = service.addContribution(1L, 10L, request);

    assertThat(result.contribution().getAmountCents()).isEqualTo(200L);
    assertThat(result.projection().savedSoFarCents()).isEqualTo(200L);
    verify(alertService).evaluateGoal(goal, 200L, YearMonth.of(2024, 8));
  }

  @Test
  void deleteContributionRecalculatesProjection() {
    Goal goal = org.mockito.Mockito.mock(Goal.class);

    GoalContribution contribution = new GoalContribution();
    contribution.setGoal(goal);
    contribution.setContributedAt(LocalDate.of(2024, 8, 5));

    when(contributionRepository.findByIdAndUserIdAndGoalId(100L, 1L, 10L))
      .thenReturn(java.util.Optional.of(contribution));
    when(projectionCalculator.endOfMonth(YearMonth.of(2024, 8))).thenReturn(LocalDate.of(2024, 8, 31));
    when(contributionRepository.sumByGoalIdUpTo(1L, 10L, LocalDate.of(2024, 8, 31)))
      .thenReturn(0L);

    GoalProjectionResponse projection = new GoalProjectionResponse(0L, 1000L, 4, 250L, "2024-11", List.of());
    when(projectionCalculator.calculate(goal, 0L, YearMonth.of(2024, 8))).thenReturn(projection);

    var result = service.deleteContribution(1L, 10L, 100L);

    assertThat(result.savedSoFarCents()).isEqualTo(0L);
  }
}
