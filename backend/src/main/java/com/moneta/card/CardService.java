package com.moneta.card;

import com.moneta.account.Account;
import com.moneta.account.AccountRepository;
import com.moneta.auth.User;
import com.moneta.auth.UserRepository;
import com.moneta.txn.TxnRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardService {
  private final CardRepository cardRepository;
  private final AccountRepository accountRepository;
  private final UserRepository userRepository;
  private final BillingCycleService billingCycleService;
  private final TxnRepository txnRepository;

  public CardService(
    CardRepository cardRepository,
    AccountRepository accountRepository,
    UserRepository userRepository,
    BillingCycleService billingCycleService,
    TxnRepository txnRepository
  ) {
    this.cardRepository = cardRepository;
    this.accountRepository = accountRepository;
    this.userRepository = userRepository;
    this.billingCycleService = billingCycleService;
    this.txnRepository = txnRepository;
  }

  @Transactional
  public Card create(Long userId, CardDtos.CreateCardRequest request) {
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("usuário não encontrado"));

    // Validate account belongs to user
    Account account = accountRepository.findByIdAndUserId(request.accountId(), userId)
      .orElseThrow(() -> new IllegalArgumentException("conta não encontrada ou não pertence ao usuário"));

    // Validate closing day
    if (request.closingDay() < 1 || request.closingDay() > 31) {
      throw new IllegalArgumentException("dia de fechamento deve estar entre 1 e 31");
    }

    // Validate due day
    if (request.dueDay() < 1 || request.dueDay() > 31) {
      throw new IllegalArgumentException("dia de vencimento deve estar entre 1 e 31");
    }

    // Validate limit
    if (request.limitAmount().compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("limite deve ser maior ou igual a zero");
    }

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

  public List<Card> list(Long userId) {
    return cardRepository.findAllByUserIdAndIsActiveTrue(userId);
  }

  public Card get(Long userId, Long id) {
    return cardRepository.findByIdAndUserIdAndIsActiveTrue(id, userId)
      .orElseThrow(() -> new IllegalArgumentException("cartão não encontrado"));
  }

  @Transactional
  public Card update(Long userId, Long id, CardDtos.UpdateCardRequest request) {
    Card card = get(userId, id);
    
    if (request.accountId() != null) {
      Account account = accountRepository.findByIdAndUserId(request.accountId(), userId)
        .orElseThrow(() -> new IllegalArgumentException("conta não encontrada ou não pertence ao usuário"));
      card.setAccount(account);
    }
    
    if (request.name() != null) {
      card.setName(request.name());
    }
    
    if (request.brand() != null) {
      card.setBrand(request.brand());
    }
    
    if (request.last4() != null) {
      card.setLast4(request.last4());
    }
    
    if (request.limitAmount() != null) {
      if (request.limitAmount().compareTo(BigDecimal.ZERO) < 0) {
        throw new IllegalArgumentException("limite deve ser maior ou igual a zero");
      }
      card.setLimitAmount(request.limitAmount());
    }
    
    if (request.closingDay() != null) {
      if (request.closingDay() < 1 || request.closingDay() > 31) {
        throw new IllegalArgumentException("dia de fechamento deve estar entre 1 e 31");
      }
      card.setClosingDay(request.closingDay());
    }
    
    if (request.dueDay() != null) {
      if (request.dueDay() < 1 || request.dueDay() > 31) {
        throw new IllegalArgumentException("dia de vencimento deve estar entre 1 e 31");
      }
      card.setDueDay(request.dueDay());
    }
    
    card.setUpdatedAt(OffsetDateTime.now());
    return cardRepository.save(card);
  }

  @Transactional
  public void disable(Long userId, Long id) {
    Card card = get(userId, id);
    card.setActive(false);
    card.setUpdatedAt(OffsetDateTime.now());
    cardRepository.save(card);
  }

  /**
   * Gets card limit summary for all active cards of a user.
   * Shows limit usage for the current billing cycle.
   *
   * @param userId the user ID
   * @param asOfDate the date to calculate the cycle for (default: today)
   * @return list of card limit summaries
   */
  public List<CardDtos.CardLimitSummary> getLimitSummary(Long userId, LocalDate asOfDate) {
    LocalDate effectiveDate = asOfDate != null ? asOfDate : LocalDate.now();
    List<Card> cards = cardRepository.findAllByUserIdAndIsActiveTrue(userId);
    List<CardDtos.CardLimitSummary> summaries = new ArrayList<>();

    for (Card card : cards) {
      CardInvoiceDtos.BillingCycle cycle = billingCycleService.calculateBillingCycle(
        effectiveDate,
        card.getClosingDay(),
        card.getDueDay()
      );

      OffsetDateTime cycleStart = cycle.startDate().atStartOfDay().atOffset(ZoneOffset.UTC);
      OffsetDateTime cycleEnd = cycle.endDate().atStartOfDay().atOffset(ZoneOffset.UTC);

      Long usedCents = txnRepository.sumCardExpensesInCycle(card.getId(), cycleStart, cycleEnd);
      if (usedCents == null) {
        usedCents = 0L;
      }

      long limitCents = card.getLimitAmount().multiply(BigDecimal.valueOf(100)).longValue();
      long availableCents = limitCents - usedCents;
      double percentUsed = limitCents == 0 ? 0.0 : (double) usedCents / (double) limitCents * 100.0;

      summaries.add(new CardDtos.CardLimitSummary(
        card.getId(),
        card.getName(),
        limitCents,
        usedCents,
        availableCents,
        percentUsed,
        cycle.startDate().toString(),
        cycle.closingDate().toString()
      ));
    }

    return summaries;
  }
}
