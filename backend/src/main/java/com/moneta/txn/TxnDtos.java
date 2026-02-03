package com.moneta.txn;

import com.moneta.card.PaymentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.OffsetDateTime;
import java.util.UUID;

public class TxnDtos {
  public record TxnRequest(
    Long accountId,
    Long cardId,
    PaymentType paymentType,
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
  ) {
    /**
     * Backward-compatible constructor matching the previous TxnRequest arity
     * (before paymentType, cardId, importRowId and categorizationMode were added).
     * <p>
     * Existing call sites that do not supply these fields
     * will use this constructor, which defaults to PIX payment type.
     */
    public TxnRequest(
      Long accountId,
      Long amountCents,
      TxnDirection direction,
      String description,
      OffsetDateTime occurredAt,
      TxnStatus status,
      Long categoryId,
      Long subcategoryId,
      Long ruleId,
      Long importBatchId
    ) {
      this(
        accountId,
        null,
        PaymentType.PIX,
        amountCents,
        direction,
        description,
        occurredAt,
        status,
        categoryId,
        subcategoryId,
        ruleId,
        importBatchId,
        null,
        null
      );
    }
  }

  public record TxnResponse(
    Long id,
    Long accountId,
    Long cardId,
    PaymentType paymentType,
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
    TxnCategorizationMode categorizationMode,
    Long importBatchId,
    Long importRowId,
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
