package com.moneta.goal;

import com.moneta.alert.AlertService;
import com.moneta.auth.User;
import com.moneta.auth.UserRepository;
import com.moneta.goal.GoalDtos.GoalContributionRequest;
import com.moneta.goal.GoalDtos.GoalProjectionResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoalContributionService {
  private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

  private final GoalRepository goalRepository;
  private final GoalContributionRepository contributionRepository;
  private final UserRepository userRepository;
  private final AlertService alertService;
  private final GoalProjectionCalculator projectionCalculator;

  public GoalContributionService(
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
  public GoalContributionResult addContribution(Long userId, Long goalId, GoalContributionRequest request) {
    Goal goal = goalRepository.findByIdAndUserId(goalId, userId)
      .orElseThrow(() -> new IllegalArgumentException("meta não encontrada"));
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("usuário não encontrado"));

    GoalContribution contribution = new GoalContribution();
    contribution.setGoal(goal);
    contribution.setUser(user);
    contribution.setContributedAt(parseDate(request.contributedAt()));
    if (request.amountCents() == null || request.amountCents() <= 0) {
      throw new IllegalArgumentException("valor inválido");
    }
    contribution.setAmountCents(request.amountCents());
    contribution.setNote(request.note());

    GoalContribution saved = contributionRepository.save(contribution);
    YearMonth asOfMonth = YearMonth.from(saved.getContributedAt());
    long savedSoFar = contributionRepository.sumByGoalIdUpTo(userId, goalId, projectionCalculator.endOfMonth(asOfMonth));
    GoalProjectionResponse projection = projectionCalculator.calculate(goal, savedSoFar, asOfMonth);
    alertService.evaluateGoal(goal, savedSoFar, asOfMonth);
    return new GoalContributionResult(saved, projection);
  }

  @Transactional
  public GoalProjectionResponse deleteContribution(Long userId, Long goalId, Long contributionId) {
    GoalContribution contribution = contributionRepository
      .findByIdAndUserIdAndGoalId(contributionId, userId, goalId)
      .orElseThrow(() -> new IllegalArgumentException("aporte não encontrado"));
    contributionRepository.delete(contribution);
    YearMonth asOfMonth = YearMonth.from(contribution.getContributedAt());
    long savedSoFar = contributionRepository.sumByGoalIdUpTo(userId, goalId, projectionCalculator.endOfMonth(asOfMonth));
    return projectionCalculator.calculate(contribution.getGoal(), savedSoFar, asOfMonth);
  }

  public Page<GoalContribution> listContributions(
    Long userId,
    Long goalId,
    String from,
    String to,
    int page,
    int size
  ) {
    LocalDate fromDate = parseOptionalDate(from);
    LocalDate toDate = parseOptionalDate(to);
    return contributionRepository.findAllByFilters(userId, goalId, fromDate, toDate, PageRequest.of(page, size));
  }

  private LocalDate parseDate(String value) {
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

  private LocalDate parseOptionalDate(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return parseDate(value);
  }

  public record GoalContributionResult(
    GoalContribution contribution,
    GoalProjectionResponse projection
  ) {}
}
