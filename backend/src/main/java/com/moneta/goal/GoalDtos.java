package com.moneta.goal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class GoalDtos {
  public record GoalRequest(
    @NotBlank(message = "nome é obrigatório") String name,
    @NotNull(message = "valor alvo é obrigatório") Long targetAmountCents,
    @NotNull(message = "data alvo é obrigatória") String targetDate,
    String startDate,
    Integer monthlyRateBps
  ) {}

  public record GoalUpdateRequest(
    String name,
    Long targetAmountCents,
    String targetDate,
    String startDate,
    Integer monthlyRateBps,
    GoalStatus status
  ) {}

  public record GoalResponse(
    Long id,
    String name,
    Long targetAmountCents,
    String startDate,
    String targetDate,
    Integer monthlyRateBps,
    GoalStatus status,
    Long savedSoFarCents
  ) {}

  public record GoalContributionRequest(
    @NotNull(message = "data é obrigatória") String contributedAt,
    @NotNull(message = "valor é obrigatório") Long amountCents,
    String note
  ) {}

  public record GoalContributionResponse(
    Long id,
    String contributedAt,
    Long amountCents,
    String note
  ) {}

  public record GoalContributionsPageResponse(
    List<GoalContributionResponse> contributions,
    int page,
    int size,
    long totalElements
  ) {}

  public record GoalProjectionResponse(
    Long savedSoFarCents,
    Long targetAmountCents,
    int monthsRemaining,
    Long neededMonthlyCents,
    String estimatedCompletionMonth,
    List<GoalProjectionScheduleItem> schedule
  ) {}

  public record GoalProjectionScheduleItem(
    String month,
    Long savedProjectedCents,
    Long neededMonthlyCents
  ) {}
}
