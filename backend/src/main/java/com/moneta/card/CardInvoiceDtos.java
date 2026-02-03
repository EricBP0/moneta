package com.moneta.card;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CardInvoiceDtos {
  public record CardInvoiceResponse(
    Long cardId,
    int year,
    int month,
    LocalDate closingDate,
    LocalDate dueDate,
    LocalDate startDate,
    LocalDate endDate,
    Long totalAmountCents,
    BigDecimal limitAmount,
    BigDecimal availableLimit,
    List<InvoiceTransactionResponse> transactions
  ) {}

  public record InvoiceTransactionResponse(
    Long id,
    String description,
    Long amountCents,
    String occurredAt,
    Long categoryId
  ) {}

  public record BillingCycle(
    int year,
    int month,
    LocalDate closingDate,
    LocalDate dueDate,
    LocalDate startDate,
    LocalDate endDate
  ) {}
}
