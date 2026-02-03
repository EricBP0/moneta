package com.moneta.card;

import com.moneta.txn.Txn;
import com.moneta.txn.TxnRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CardInvoiceService {
  private final CardRepository cardRepository;
  private final TxnRepository txnRepository;
  private final BillingCycleService billingCycleService;

  public CardInvoiceService(
    CardRepository cardRepository,
    TxnRepository txnRepository,
    BillingCycleService billingCycleService
  ) {
    this.cardRepository = cardRepository;
    this.txnRepository = txnRepository;
    this.billingCycleService = billingCycleService;
  }

  public CardInvoiceDtos.CardInvoiceResponse getInvoice(Long userId, Long cardId, int year, int month) {
    // Validate card belongs to user
    Card card = cardRepository.findByIdAndUserIdAndIsActiveTrue(cardId, userId)
      .orElseThrow(() -> new IllegalArgumentException("cartão não encontrado"));

    // Calculate billing cycle for the requested month
    CardInvoiceDtos.BillingCycle cycle = billingCycleService.calculateBillingCycleForMonth(
      year, 
      month, 
      card.getClosingDay(), 
      card.getDueDay()
    );

    // Convert LocalDate to OffsetDateTime for querying
    OffsetDateTime startDateTime = cycle.startDate().atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
    OffsetDateTime endDateTime = cycle.endDate().atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

    // Get transactions for this billing cycle
    List<Txn> transactions = txnRepository.findCardTransactionsForInvoice(
      cardId,
      startDateTime,
      endDateTime
    );

    // Calculate total (only OUT/expenses for credit cards)
    long totalAmountCents = transactions.stream()
      .filter(txn -> txn.getDirection() == com.moneta.txn.TxnDirection.OUT)
      .mapToLong(Txn::getAmountCents)
      .sum();

    // Convert transactions to response DTOs
    List<CardInvoiceDtos.InvoiceTransactionResponse> txnResponses = transactions.stream()
      .map(txn -> new CardInvoiceDtos.InvoiceTransactionResponse(
        txn.getId(),
        txn.getDescription(),
        txn.getAmountCents(),
        txn.getOccurredAt().toString(),
        txn.getCategoryId()
      ))
      .toList();

    // Calculate available limit
    BigDecimal totalSpent = BigDecimal.valueOf(totalAmountCents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    BigDecimal availableLimit = card.getLimitAmount().subtract(totalSpent);
    if (availableLimit.compareTo(BigDecimal.ZERO) < 0) {
      availableLimit = BigDecimal.ZERO;
    }

    return new CardInvoiceDtos.CardInvoiceResponse(
      cardId,
      year,
      month,
      cycle.closingDate(),
      cycle.dueDate(),
      cycle.startDate(),
      cycle.endDate(),
      totalAmountCents,
      card.getLimitAmount(),
      availableLimit,
      txnResponses
    );
  }
}
