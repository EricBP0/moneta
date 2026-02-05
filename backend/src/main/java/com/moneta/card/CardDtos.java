package com.moneta.card;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class CardDtos {
  public record CreateCardRequest(
    @NotNull(message = "conta é obrigatória") Long accountId,
    @NotBlank(message = "nome é obrigatório") String name,
    String brand,
    String last4,
    @NotNull(message = "limite é obrigatório") @PositiveOrZero(message = "limite deve ser maior ou igual a zero") BigDecimal limitAmount,
    @NotNull(message = "dia de fechamento é obrigatório") @Min(value = 1, message = "dia de fechamento deve estar entre 1 e 31") @Max(value = 31, message = "dia de fechamento deve estar entre 1 e 31") Integer closingDay,
    @NotNull(message = "dia de vencimento é obrigatório") @Min(value = 1, message = "dia de vencimento deve estar entre 1 e 31") @Max(value = 31, message = "dia de vencimento deve estar entre 1 e 31") Integer dueDay
  ) {}

  public record UpdateCardRequest(
    Long accountId,
    String name,
    String brand,
    String last4,
    BigDecimal limitAmount,
    Integer closingDay,
    Integer dueDay
  ) {}

  public record CardResponse(
    Long id,
    Long accountId,
    String accountName,
    String name,
    String brand,
    String last4,
    BigDecimal limitAmount,
    Integer closingDay,
    Integer dueDay,
    boolean isActive,
    OffsetDateTime createdAt
  ) {}

  public record CardLimitSummary(
    Long cardId,
    String cardName,
    long limitTotal,
    long usedCents,
    long availableCents,
    double percentUsed,
    String cycleStart,
    String cycleClosing
  ) {}
}
