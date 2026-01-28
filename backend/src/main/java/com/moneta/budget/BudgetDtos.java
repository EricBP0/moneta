package com.moneta.budget;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class BudgetDtos {
  public record BudgetRequest(
    @NotBlank(message = "mês é obrigatório") String monthRef,
    Long categoryId,
    Long subcategoryId,
    @NotNull(message = "limite é obrigatório") @Positive(message = "limite deve ser positivo") Long limitCents
  ) {}

  public record BudgetResponse(
    Long id,
    String monthRef,
    Long categoryId,
    Long subcategoryId,
    Long limitCents
  ) {}
}
