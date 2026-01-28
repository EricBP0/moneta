package com.moneta.txn;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.OffsetDateTime;
import java.util.UUID;

public class TxnDtos {
  public record TxnRequest(
    @NotNull(message = "conta é obrigatória") Long accountId,
    @NotNull(message = "valor é obrigatório") @Positive(message = "valor deve ser positivo") Long amountCents,
    @NotNull(message = "direção é obrigatória") TxnDirection direction,
    String description,
    @NotNull(message = "data é obrigatória") OffsetDateTime occurredAt,
    TxnStatus status,
    Long categoryId,
    Long subcategoryId,
    Long ruleId,
    Long importBatchId,
    Long importRowId,
    TxnCategorizationMode categorizationMode
  ) {}

  public record TxnResponse(
    Long id,
    Long accountId,
    Long amountCents,
    TxnDirection direction,
    String description,
    OffsetDateTime occurredAt,
    String monthRef,
    TxnStatus status,
    TxnType txnType,
    Long categoryId,
    Long subcategoryId,
    Long ruleId,
    Long importBatchId,
    Long importRowId,
    TxnCategorizationMode categorizationMode,
    UUID transferGroupId,
    boolean isActive
  ) {}

  public record TxnFilter(
    String monthRef,
    Long accountId,
    Long categoryId,
    String query,
    TxnDirection direction,
    TxnStatus status
  ) {}

  public record TransferRequest(
    @NotNull(message = "conta origem é obrigatória") Long fromAccountId,
    @NotNull(message = "conta destino é obrigatória") Long toAccountId,
    @NotNull(message = "valor é obrigatório") @Positive(message = "valor deve ser positivo") Long amountCents,
    @NotNull(message = "data é obrigatória") OffsetDateTime occurredAt,
    String description
  ) {}

  public record TransferResponse(
    UUID transferGroupId,
    TxnResponse outgoing,
    TxnResponse incoming
  ) {}
}
