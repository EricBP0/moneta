package com.moneta.rule;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class RuleDtos {
  public record RuleRequest(
    @NotBlank(message = "nome é obrigatório") String name,
    @NotNull(message = "prioridade é obrigatória") @Min(value = 0, message = "prioridade inválida") Integer priority,
    @NotNull(message = "tipo de match é obrigatório") RuleMatchType matchType,
    @NotBlank(message = "padrão é obrigatório") String pattern,
    Long categoryId,
    Long subcategoryId,
    Long accountId,
    Boolean isActive
  ) {}

  public record RuleResponse(
    Long id,
    String name,
    Integer priority,
    RuleMatchType matchType,
    String pattern,
    Long categoryId,
    Long subcategoryId,
    Long accountId,
    boolean isActive
  ) {}

  public record RuleApplyRequest(
    String month,
    Long accountId,
    Boolean onlyUncategorized,
    Boolean dryRun,
    Boolean overrideManual
  ) {}

  public record RuleApplyDetail(
    Long txnId,
    Long ruleId,
    Long categoryId,
    Long subcategoryId
  ) {}

  public record RuleApplyResponse(
    int evaluated,
    int matched,
    int updated,
    int skippedManual,
    List<RuleApplyDetail> detailsSample
  ) {}
}
