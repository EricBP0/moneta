package com.moneta.card;

import com.moneta.account.Account;
import com.moneta.account.AccountRepository;
import com.moneta.auth.User;
import com.moneta.auth.UserRepository;
import com.moneta.card.BillingCycleCalculator.BillingCycle;
import com.moneta.card.CardDtos.CardInvoiceResponse;
import com.moneta.card.CardDtos.CardInvoiceTransaction;
import com.moneta.txn.Txn;
import com.moneta.txn.TxnRepository;
import com.moneta.txn.TxnDirection;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardService {
  private final CardRepository cardRepository;
  private final UserRepository userRepository;
  private final AccountRepository accountRepository;
  private final TxnRepository txnRepository;

  public CardService(
    CardRepository cardRepository,
    UserRepository userRepository,
    AccountRepository accountRepository,
    TxnRepository txnRepository
  ) {
    this.cardRepository = cardRepository;
    this.userRepository = userRepository;
    this.accountRepository = accountRepository;
    this.txnRepository = txnRepository;
  }

  @Transactional
  public Card createCard(Long userId, CardDtos.CreateCardRequest request) {
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("usuário não encontrado"));
    Account account = accountRepository.findByIdAndUserId(request.accountId(), userId)
      .orElseThrow(() -> new IllegalArgumentException("conta não encontrada"));
    validateRequest(request.limitAmount(), request.closingDay(), request.dueDay());

    Card card = new Card();
    card.setUser(user);
    card.setAccount(account);
    card.setName(request.name());
    card.setBrand(request.brand());
    card.setLast4(request.last4());
    card.setLimitAmount(request.limitAmount());
    card.setClosingDay(request.closingDay());
    card.setDueDay(request.dueDay());
    return cardRepository.save(card);
  }

  public List<Card> listCards(Long userId) {
    return cardRepository.findAllByUserIdAndActiveTrue(userId);
  }

  public Card getCard(Long userId, Long cardId) {
    return cardRepository.findByIdAndUserId(cardId, userId)
      .orElseThrow(() -> new IllegalArgumentException("cartão não encontrado"));
  }

  @Transactional
  public Card updateCard(Long userId, Long cardId, CardDtos.UpdateCardRequest request) {
    Card card = getCard(userId, cardId);
    Account account = accountRepository.findByIdAndUserId(request.accountId(), userId)
      .orElseThrow(() -> new IllegalArgumentException("conta não encontrada"));
    validateRequest(request.limitAmount(), request.closingDay(), request.dueDay());

    card.setAccount(account);
    card.setName(request.name());
    card.setBrand(request.brand());
    card.setLast4(request.last4());
    card.setLimitAmount(request.limitAmount());
    card.setClosingDay(request.closingDay());
    card.setDueDay(request.dueDay());
    card.setUpdatedAt(OffsetDateTime.now());
    return cardRepository.save(card);
  }

  @Transactional
  public void disableCard(Long userId, Long cardId) {
    Card card = getCard(userId, cardId);
    card.setActive(false);
    card.setUpdatedAt(OffsetDateTime.now());
    cardRepository.save(card);
  }

  public CardInvoiceResponse getInvoice(Long userId, Long cardId, int year, int month) {
    Card card = getCard(userId, cardId);
    if (!card.isActive()) {
      throw new IllegalArgumentException("cartão desativado");
    }
    BillingCycle cycle = BillingCycleCalculator.forMonth(year, month, card.getClosingDay(), card.getDueDay());
    OffsetDateTime start = cycle.startDate().atStartOfDay().atOffset(ZoneOffset.UTC);
    OffsetDateTime endExclusive = cycle.endDateExclusive().atStartOfDay().atOffset(ZoneOffset.UTC);
    List<Txn> txns = txnRepository.findCardTransactionsByUserAndCardAndPeriod(
      userId,
      card.getId(),
      start,
      endExclusive
    );
    long totalAmount = txns.stream()
      .mapToLong(txn -> txn.getDirection() == TxnDirection.OUT ? txn.getAmountCents() : -txn.getAmountCents())
      .sum();
    List<CardInvoiceTransaction> transactions = txns.stream()
      .map(txn -> new CardInvoiceTransaction(
        txn.getId(),
        txn.getAmountCents(),
        txn.getDirection(),
        txn.getDescription(),
        txn.getOccurredAt(),
        txn.getStatus()
      ))
      .toList();

    return new CardInvoiceResponse(
      card.getId(),
      cycle.month(),
      cycle.year(),
      cycle.closingDate(),
      cycle.dueDate(),
      totalAmount,
      transactions,
      card.getLimitAmount()
    );
  }

  private void validateRequest(BigDecimal limitAmount, int closingDay, int dueDay) {
    if (limitAmount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("limite deve ser positivo");
    }
    if (closingDay < 1 || closingDay > 31) {
      throw new IllegalArgumentException("dia de fechamento inválido");
    }
    if (dueDay < 1 || dueDay > 31) {
      throw new IllegalArgumentException("dia de vencimento inválido");
    }
  }
}
