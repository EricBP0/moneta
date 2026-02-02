package com.moneta.card;

import com.moneta.txn.TxnDirection;
import com.moneta.txn.TxnStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class CardDtos {
  public record CreateCardRequest(
    @NotNull(message = "conta é obrigatória") Long accountId,
    @NotBlank(message = "nome é obrigatório") String name,
    String brand,
    String last4,
    @NotNull(message = "limite é obrigatório")
    @PositiveOrZero(message = "limite deve ser positivo")
    BigDecimal limitAmount,
    @NotNull(message = "dia de fechamento é obrigatório") @Min(1) @Max(31) Integer closingDay,
    @NotNull(message = "dia de vencimento é obrigatório") @Min(1) @Max(31) Integer dueDay
  ) {}

  public record UpdateCardRequest(
    @NotNull(message = "conta é obrigatória") Long accountId,
    @NotBlank(message = "nome é obrigatório") String name,
    String brand,
    String last4,
    @NotNull(message = "limite é obrigatório")
    @PositiveOrZero(message = "limite deve ser positivo")
    BigDecimal limitAmount,
    @NotNull(message = "dia de fechamento é obrigatório") @Min(1) @Max(31) Integer closingDay,
    @NotNull(message = "dia de vencimento é obrigatório") @Min(1) @Max(31) Integer dueDay
  ) {}

  public record CardResponse(
    Long id,
    Long accountId,
    String name,
    String brand,
    String last4,
    BigDecimal limitAmount,
    int closingDay,
    int dueDay,
    boolean active
  ) {}

  public record CardInvoiceTransaction(
    Long id,
    Long amountCents,
    TxnDirection direction,
    String description,
    OffsetDateTime occurredAt,
    TxnStatus status
  ) {}

  public record CardInvoiceResponse(
    Long cardId,
    int month,
    int year,
    LocalDate closingDate,
    LocalDate dueDate,
    Long totalAmountCents,
    List<CardInvoiceTransaction> transactions,
    BigDecimal limitAmount
  ) {}
}
