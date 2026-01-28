package com.moneta.goal;

import com.moneta.alert.AlertService;
import com.moneta.auth.User;
import com.moneta.auth.UserRepository;
import com.moneta.goal.GoalDtos.GoalProjectionResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoalService {
  private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

  private final GoalRepository goalRepository;
  private final GoalContributionRepository contributionRepository;
  private final UserRepository userRepository;
  private final AlertService alertService;
  private final GoalProjectionCalculator projectionCalculator;

  public GoalService(
    GoalRepository goalRepository,
    GoalContributionRepository contributionRepository,
    UserRepository userRepository,
    AlertService alertService,
    GoalProjectionCalculator projectionCalculator
  ) {
    this.goalRepository = goalRepository;
    this.contributionRepository = contributionRepository;
    this.userRepository = userRepository;
    this.alertService = alertService;
    this.projectionCalculator = projectionCalculator;
  }

  @Transactional
  public Goal create(Long userId, GoalDtos.GoalRequest request) {
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("usuário não encontrado"));
    Goal goal = new Goal();
    goal.setUser(user);
    applyRequest(goal, request.name(), request.targetAmountCents(), request.targetDate(), request.startDate(), request.monthlyRateBps());
    Goal saved = goalRepository.save(goal);
    long savedSoFar = contributionRepository.sumByGoalId(userId, saved.getId());
    alertService.evaluateGoal(saved, savedSoFar, YearMonth.now());
    return saved;
  }

  public List<Goal> list(Long userId) {
    return goalRepository.findAllByUserId(userId);
  }

  public Goal get(Long userId, Long goalId) {
    return goalRepository.findByIdAndUserId(goalId, userId)
      .orElseThrow(() -> new IllegalArgumentException("meta não encontrada"));
  }

  @Transactional
  public Goal update(Long userId, Long goalId, GoalDtos.GoalUpdateRequest request) {
    Goal goal = get(userId, goalId);
    if (request.name() != null && !request.name().isBlank()) {
      goal.setName(request.name());
    }
    if (request.targetAmountCents() != null) {
      goal.setTargetAmountCents(request.targetAmountCents());
    }
    if (request.targetDate() != null) {
      goal.setTargetDate(parseGoalDate(request.targetDate()));
    }
    if (request.startDate() != null) {
      goal.setStartDate(parseGoalDate(request.startDate()));
    }
    if (request.monthlyRateBps() != null) {
      goal.setMonthlyRateBps(request.monthlyRateBps());
    }
    if (request.status() != null) {
      goal.setStatus(request.status());
    }
    validateDates(goal.getStartDate(), goal.getTargetDate());
    Goal saved = goalRepository.save(goal);
    long savedSoFar = contributionRepository.sumByGoalId(userId, goalId);
    alertService.evaluateGoal(saved, savedSoFar, YearMonth.now());
    return saved;
  }

  @Transactional
  public void cancel(Long userId, Long goalId) {
    Goal goal = get(userId, goalId);
    goal.setStatus(GoalStatus.CANCELED);
    goalRepository.save(goal);
  }

  public GoalProjectionResponse getProjection(Long userId, Goal goal, YearMonth asOfMonth) {
    long savedSoFar = contributionRepository.sumByGoalIdUpTo(
      userId,
      goal.getId(),
      projectionCalculator.endOfMonth(asOfMonth)
    );
    return projectionCalculator.calculate(goal, savedSoFar, asOfMonth);
  }

  public Map<Long, Long> getSavedTotals(Long userId, List<Goal> goals) {
    if (goals.isEmpty()) {
      return Map.of();
    }
    List<Long> goalIds = goals.stream().map(Goal::getId).toList();
    return contributionRepository.sumTotalsByGoalIds(userId, goalIds).stream()
      .collect(Collectors.toMap(
        GoalContributionRepository.GoalContributionTotalProjection::getGoalId,
        GoalContributionRepository.GoalContributionTotalProjection::getTotalCents
      ));
  }

  private void applyRequest(
    Goal goal,
    String name,
    Long targetAmountCents,
    String targetDate,
    String startDate,
    Integer monthlyRateBps
  ) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("nome é obrigatório");
    }
    if (targetAmountCents == null || targetAmountCents <= 0) {
      throw new IllegalArgumentException("valor alvo inválido");
    }
    goal.setName(name);
    goal.setTargetAmountCents(targetAmountCents);
    goal.setTargetDate(parseGoalDate(targetDate));
    LocalDate start = startDate == null || startDate.isBlank()
      ? LocalDate.now()
      : parseGoalDate(startDate);
    goal.setStartDate(start);
    goal.setMonthlyRateBps(monthlyRateBps == null ? 0 : monthlyRateBps);
    validateDates(goal.getStartDate(), goal.getTargetDate());
  }

  private LocalDate parseGoalDate(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("data inválida");
    }
    try {
      if (value.length() == 7) {
        YearMonth month = YearMonth.parse(value, MONTH_FORMATTER);
        return month.atDay(1);
      }
      return LocalDate.parse(value);
    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException("data inválida");
    }
  }

  private void validateDates(LocalDate startDate, LocalDate targetDate) {
    if (targetDate.isBefore(startDate)) {
      throw new IllegalArgumentException("data alvo deve ser após data inicial");
    }
  }
}
