package com.moneta.goal;

import com.moneta.config.UserPrincipal;
import com.moneta.goal.GoalContributionService.GoalContributionResult;
import com.moneta.goal.GoalDtos.GoalContributionRequest;
import com.moneta.goal.GoalDtos.GoalContributionResponse;
import com.moneta.goal.GoalDtos.GoalContributionsPageResponse;
import com.moneta.goal.GoalDtos.GoalProjectionResponse;
import com.moneta.goal.GoalDtos.GoalRequest;
import com.moneta.goal.GoalDtos.GoalResponse;
import com.moneta.goal.GoalDtos.GoalUpdateRequest;
import jakarta.validation.Valid;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goals")
public class GoalController {
  private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

  private final GoalService goalService;
  private final GoalContributionService contributionService;

  public GoalController(GoalService goalService, GoalContributionService contributionService) {
    this.goalService = goalService;
    this.contributionService = contributionService;
  }

  @GetMapping
  public List<GoalResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
    List<Goal> goals = goalService.list(principal.getId());
    Map<Long, Long> totals = goalService.getSavedTotals(principal.getId(), goals);
    return goals.stream()
      .map(goal -> toResponse(goal, totals.getOrDefault(goal.getId(), 0L)))
      .toList();
  }

  @PostMapping
  public GoalResponse create(
    @AuthenticationPrincipal UserPrincipal principal,
    @Valid @RequestBody GoalRequest request
  ) {
    Goal goal = goalService.create(principal.getId(), request);
    long savedSoFar = goalService.getSavedTotals(principal.getId(), List.of(goal)).getOrDefault(goal.getId(), 0L);
    return toResponse(goal, savedSoFar);
  }

  @GetMapping("/{id}")
  public GoalResponse get(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id
  ) {
    Goal goal = goalService.get(principal.getId(), id);
    long savedSoFar = goalService.getSavedTotals(principal.getId(), List.of(goal)).getOrDefault(goal.getId(), 0L);
    return toResponse(goal, savedSoFar);
  }

  @PatchMapping("/{id}")
  public GoalResponse update(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id,
    @RequestBody GoalUpdateRequest request
  ) {
    Goal goal = goalService.update(principal.getId(), id, request);
    long savedSoFar = goalService.getSavedTotals(principal.getId(), List.of(goal)).getOrDefault(goal.getId(), 0L);
    return toResponse(goal, savedSoFar);
  }

  @DeleteMapping("/{id}")
  public void cancel(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id
  ) {
    goalService.cancel(principal.getId(), id);
  }

  @GetMapping("/{id}/projection")
  public GoalProjectionResponse projection(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id,
    @RequestParam(required = false) String asOf
  ) {
    Goal goal = goalService.get(principal.getId(), id);
    YearMonth asOfMonth = asOf == null || asOf.isBlank()
      ? YearMonth.now()
      : YearMonth.parse(asOf, MONTH_FORMATTER);
    return goalService.getProjection(principal.getId(), goal, asOfMonth);
  }

  @GetMapping("/{id}/contributions")
  public GoalContributionsPageResponse listContributions(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id,
    @RequestParam(required = false) String from,
    @RequestParam(required = false) String to,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    Page<GoalContribution> contributions = contributionService.listContributions(
      principal.getId(),
      id,
      from,
      to,
      page,
      size
    );
    return new GoalContributionsPageResponse(
      contributions.getContent().stream().map(this::toContributionResponse).toList(),
      page,
      size,
      contributions.getTotalElements()
    );
  }

  @PostMapping("/{id}/contributions")
  public GoalContributionWithProjection addContribution(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id,
    @Valid @RequestBody GoalContributionRequest request
  ) {
    GoalContributionResult result = contributionService.addContribution(principal.getId(), id, request);
    return new GoalContributionWithProjection(
      toContributionResponse(result.contribution()),
      result.projection()
    );
  }

  @DeleteMapping("/{id}/contributions/{contributionId}")
  public GoalProjectionResponse deleteContribution(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id,
    @PathVariable Long contributionId
  ) {
    return contributionService.deleteContribution(principal.getId(), id, contributionId);
  }

  private GoalResponse toResponse(Goal goal, long savedSoFar) {
    return new GoalResponse(
      goal.getId(),
      goal.getName(),
      goal.getTargetAmountCents(),
      goal.getStartDate().toString(),
      goal.getTargetDate().format(MONTH_FORMATTER),
      goal.getMonthlyRateBps(),
      goal.getStatus(),
      savedSoFar
    );
  }

  private GoalContributionResponse toContributionResponse(GoalContribution contribution) {
    return new GoalContributionResponse(
      contribution.getId(),
      contribution.getContributedAt().toString(),
      contribution.getAmountCents(),
      contribution.getNote()
    );
  }

  public record GoalContributionWithProjection(
    GoalContributionResponse contribution,
    GoalProjectionResponse projection
  ) {}
}
