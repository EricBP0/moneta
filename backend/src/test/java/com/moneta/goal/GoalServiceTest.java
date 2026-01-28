package com.moneta.goal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moneta.alert.AlertService;
import com.moneta.auth.User;
import com.moneta.auth.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {
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

  private GoalService goalService;

  @BeforeEach
  void setup() {
    goalService = new GoalService(
      goalRepository,
      contributionRepository,
      userRepository,
      alertService,
      projectionCalculator
    );
  }

  @Test
  void listUsesUserFilter() {
    Goal goal = new Goal();
    when(goalRepository.findAllByUserId(1L)).thenReturn(List.of(goal));

    List<Goal> results = goalService.list(1L);

    assertThat(results).hasSize(1);
    verify(goalRepository).findAllByUserId(1L);
  }

  @Test
  void createStoresGoalForUser() {
    User user = new User();
    when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));
    
    // Mock the saved goal to return an ID
    Goal mockSavedGoal = org.mockito.Mockito.mock(Goal.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
    when(mockSavedGoal.getId()).thenReturn(1L);
    when(mockSavedGoal.getName()).thenReturn("Reserva");
    
    when(goalRepository.save(any(Goal.class))).thenReturn(mockSavedGoal);
    when(contributionRepository.sumByGoalId(1L, 1L)).thenReturn(0L);

    GoalDtos.GoalRequest request = new GoalDtos.GoalRequest(
      "Reserva",
      5000L,
      "2100-12",
      "2100-01-01",
      0
    );

    Goal goal = goalService.create(1L, request);

    assertThat(goal.getName()).isEqualTo("Reserva");
    verify(goalRepository).save(any(Goal.class));
  }

  @Test
  void getSavedTotalsAggregatesByGoal() {
    Goal goal = org.mockito.Mockito.mock(Goal.class);
    when(goal.getId()).thenReturn(10L);
    GoalContributionRepository.GoalContributionTotalProjection projection =
      new GoalContributionRepository.GoalContributionTotalProjection() {
        @Override
        public Long getGoalId() {
          return 10L;
        }

        @Override
        public Long getTotalCents() {
          return 1500L;
        }
      };
    when(contributionRepository.sumTotalsByGoalIds(1L, List.of(10L))).thenReturn(List.of(projection));

    var totals = goalService.getSavedTotals(1L, List.of(goal));

    assertThat(totals.get(10L)).isEqualTo(1500L);
  }

  @Test
  void updateValidatesDates() {
    Goal goal = new Goal();
    goal.setStartDate(LocalDate.of(2024, 1, 1));
    goal.setTargetDate(LocalDate.of(2024, 12, 1));
    when(goalRepository.findByIdAndUserId(10L, 1L)).thenReturn(java.util.Optional.of(goal));

    GoalDtos.GoalUpdateRequest request = new GoalDtos.GoalUpdateRequest(
      null,
      null,
      "2023-01",
      "2024-01",
      null,
      null
    );

    assertThatThrownBy(() -> goalService.update(1L, 10L, request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("data alvo");
  }
}
