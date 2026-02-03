package com.moneta.txn;

import com.moneta.account.Account;
import com.moneta.account.AccountRepository;
import com.moneta.auth.User;
import com.moneta.auth.UserRepository;
import com.moneta.card.Card;
import com.moneta.card.CardRepository;
import com.moneta.card.PaymentType;
import com.moneta.category.CategoryRepository;
import com.moneta.alert.AlertService;
import com.moneta.txn.TxnDtos.TxnFilter;
import com.moneta.txn.TxnDtos.TxnRequest;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TxnService {
  private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
  private static final Logger logger = LoggerFactory.getLogger(TxnService.class);

  private final TxnRepository txnRepository;
  private final UserRepository userRepository;
  private final AccountRepository accountRepository;
  private final CardRepository cardRepository;
  private final CategoryRepository categoryRepository;
  private final AlertService alertService;

  public TxnService(
    TxnRepository txnRepository,
    UserRepository userRepository,
    AccountRepository accountRepository,
    CardRepository cardRepository,
    CategoryRepository categoryRepository,
    AlertService alertService
  ) {
    this.txnRepository = txnRepository;
    this.userRepository = userRepository;
    this.accountRepository = accountRepository;
    this.cardRepository = cardRepository;
    this.categoryRepository = categoryRepository;
    this.alertService = alertService;
  }

  @Transactional
  public Txn create(Long userId, TxnRequest request) {
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("usuário não encontrado"));
    
    // Default to PIX if not specified
    PaymentType paymentType = request.paymentType() != null ? request.paymentType() : PaymentType.PIX;
    
    // Validate payment type constraints
    validatePaymentTypeConstraints(userId, paymentType, request.accountId(), request.cardId());
    
    Account account = null;
    Card card = null;
    
    if (paymentType == PaymentType.PIX) {
      account = accountRepository.findByIdAndUserId(request.accountId(), userId)
        .orElseThrow(() -> new IllegalArgumentException("conta não encontrada"));
    } else if (paymentType == PaymentType.CARD) {
      card = cardRepository.findByIdAndUserIdAndIsActiveTrue(request.cardId(), userId)
        .orElseThrow(() -> new IllegalArgumentException("cartão não encontrado ou inativo"));
      // Card must be linked to an account
      account = card.getAccount();
    }
    
    validateCategory(userId, request.categoryId());

    Txn txn = new Txn();
    txn.setUser(user);
    txn.setAccount(account);
    txn.setPaymentType(paymentType);
    txn.setCard(card);
    txn.setAmountCents(request.amountCents());
    txn.setDirection(request.direction());
    txn.setDescription(request.description());
    txn.setOccurredAt(request.occurredAt());
    txn.setMonthRef(request.occurredAt().format(MONTH_FORMATTER));
    txn.setStatus(request.status() == null ? TxnStatus.POSTED : request.status());
    txn.setTxnType(TxnType.NORMAL);
    txn.setCategoryId(request.categoryId());
    txn.setSubcategoryId(request.subcategoryId());
    txn.setRuleId(request.ruleId());
    txn.setCategorizationMode(resolveCategorizationMode(request));
    txn.setImportBatchId(request.importBatchId());
    Txn saved = txnRepository.save(txn);
    logger.info(
      "Transaction created userId={} txnId={} accountId={} cardId={} paymentType={} amountCents={} direction={} occurredAt={} categoryId={}",
      userId,
      saved.getId(),
      account != null ? account.getId() : null,
      card != null ? card.getId() : null,
      paymentType,
      saved.getAmountCents(),
      saved.getDirection(),
      saved.getOccurredAt(),
      saved.getCategoryId()
    );
    alertService.evaluateBudgetsForTxn(saved);
    return saved;
  }

  public List<Txn> list(Long userId, TxnFilter filter) {
    Specification<Txn> spec = (root, query, cb) -> {
      List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.get("user").get("id"), userId));
      predicates.add(cb.isTrue(root.get("isActive")));

      if (filter.monthRef() != null && !filter.monthRef().isBlank()) {
        predicates.add(cb.equal(root.get("monthRef"), filter.monthRef()));
      }
      if (filter.accountId() != null) {
        predicates.add(cb.equal(root.get("account").get("id"), filter.accountId()));
      }
      if (filter.categoryId() != null) {
        predicates.add(cb.equal(root.get("categoryId"), filter.categoryId()));
      }
      if (filter.direction() != null) {
        predicates.add(cb.equal(root.get("direction"), filter.direction()));
      }
      if (filter.status() != null) {
        predicates.add(cb.equal(root.get("status"), filter.status()));
      }
      if (filter.query() != null && !filter.query().isBlank()) {
        predicates.add(cb.like(
          cb.lower(root.get("description")),
          "%" + filter.query().toLowerCase(Locale.ROOT) + "%"
        ));
      }
      return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
    };
    return txnRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "occurredAt"));
  }

  public Txn get(Long userId, Long id) {
    return txnRepository.findByIdAndUserIdAndIsActiveTrue(id, userId)
      .orElseThrow(() -> new IllegalArgumentException("transação não encontrada"));
  }

  @Transactional
  public Txn update(Long userId, Long id, TxnRequest request) {
    Txn txn = get(userId, id);
    
    // Default to PIX if not specified
    PaymentType paymentType = request.paymentType() != null ? request.paymentType() : PaymentType.PIX;
    
    // Validate payment type constraints
    validatePaymentTypeConstraints(userId, paymentType, request.accountId(), request.cardId());
    
    Account account = null;
    Card card = null;
    
    if (paymentType == PaymentType.PIX) {
      account = accountRepository.findByIdAndUserId(request.accountId(), userId)
        .orElseThrow(() -> new IllegalArgumentException("conta não encontrada"));
    } else if (paymentType == PaymentType.CARD) {
      card = cardRepository.findByIdAndUserIdAndIsActiveTrue(request.cardId(), userId)
        .orElseThrow(() -> new IllegalArgumentException("cartão não encontrado ou inativo"));
      // Card must be linked to an account
      account = card.getAccount();
    }
    
    validateCategory(userId, request.categoryId());

    txn.setAccount(account);
    txn.setPaymentType(paymentType);
    txn.setCard(card);
    txn.setAmountCents(request.amountCents());
    txn.setDirection(request.direction());
    txn.setDescription(request.description());
    txn.setOccurredAt(request.occurredAt());
    txn.setMonthRef(request.occurredAt().format(MONTH_FORMATTER));
    txn.setStatus(request.status() == null ? txn.getStatus() : request.status());
    txn.setCategoryId(request.categoryId());
    txn.setSubcategoryId(request.subcategoryId());
    txn.setRuleId(request.ruleId());
    txn.setCategorizationMode(resolveCategorizationMode(request));
    txn.setImportBatchId(request.importBatchId());
    Txn saved = txnRepository.save(txn);
    alertService.evaluateBudgetsForTxn(saved);
    return saved;
  }

  @Transactional
  public void softDelete(Long userId, Long id) {
    Txn txn = get(userId, id);
    txn.setActive(false);
    txnRepository.save(txn);
  }

  private void validateCategory(Long userId, Long categoryId) {
    if (Objects.isNull(categoryId)) {
      return;
    }
    categoryRepository.findByIdAndUserId(categoryId, userId)
      .orElseThrow(() -> new IllegalArgumentException("categoria não encontrada"));
  }

  private TxnCategorizationMode resolveCategorizationMode(TxnRequest request) {
    if (request.ruleId() != null) {
      return TxnCategorizationMode.RULE;
    }
    if (request.categoryId() != null || request.subcategoryId() != null) {
      return TxnCategorizationMode.MANUAL;
    }
    // Return null for uncategorized transactions so they can be distinguished from manual ones
    return null;
  }

  private void validatePaymentTypeConstraints(Long userId, PaymentType paymentType, Long accountId, Long cardId) {
    if (paymentType == PaymentType.PIX) {
      if (accountId == null) {
        throw new IllegalArgumentException("transação PIX requer uma conta");
      }
      if (cardId != null) {
        throw new IllegalArgumentException("transação PIX não pode ter cartão");
      }
    } else if (paymentType == PaymentType.CARD) {
      if (cardId == null) {
        throw new IllegalArgumentException("transação CARD requer um cartão");
      }
      // For CARD transactions, account is derived from the card, not provided directly
      if (accountId != null) {
        throw new IllegalArgumentException("transação CARD não deve ter conta diretamente (é obtida via cartão)");
      }
    }
  }
}
