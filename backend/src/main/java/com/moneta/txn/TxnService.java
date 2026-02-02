package com.moneta.txn;

import com.moneta.account.Account;
import com.moneta.account.AccountRepository;
import com.moneta.alert.AlertService;
import com.moneta.auth.User;
import com.moneta.auth.UserRepository;
import com.moneta.card.Card;
import com.moneta.card.CardRepository;
import com.moneta.category.CategoryRepository;
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
    PaymentType paymentType = request.paymentType() == null ? PaymentType.PIX : request.paymentType();
    Card card = null;
    Account account = null;
    if (paymentType == PaymentType.PIX) {
      if (request.accountId() == null) {
        throw new IllegalArgumentException("conta é obrigatória para PIX");
      }
      if (request.cardId() != null) {
        throw new IllegalArgumentException("cartão deve ser nulo para PIX");
      }
      account = accountRepository.findByIdAndUserId(request.accountId(), userId)
        .orElseThrow(() -> new IllegalArgumentException("conta não encontrada"));
    } else if (paymentType == PaymentType.CARD) {
      if (request.cardId() == null) {
        throw new IllegalArgumentException("cartão é obrigatório para pagamento com cartão");
      }
      if (request.accountId() != null) {
        throw new IllegalArgumentException("conta deve ser nula para cartão");
      }
      card = cardRepository.findByIdAndUserId(request.cardId(), userId)
        .orElseThrow(() -> new IllegalArgumentException("cartão não encontrado"));
      if (!card.isActive()) {
        throw new IllegalArgumentException("cartão desativado");
      }
      if (!Objects.equals(card.getAccount().getUser().getId(), userId)) {
        throw new IllegalArgumentException("cartão não pertence ao usuário");
      }
    }
    validateCategory(userId, request.categoryId());

    Txn txn = new Txn();
    txn.setUser(user);
    txn.setAccount(account);
    txn.setCard(card);
    txn.setPaymentType(paymentType);
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
      "Transaction created userId={} txnId={} accountId={} cardId={} amountCents={} direction={} occurredAt={} categoryId={}",
      userId,
      saved.getId(),
      account != null ? account.getId() : null,
      card != null ? card.getId() : null,
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
        predicates.add(cb.equal(root.get("paymentType"), PaymentType.PIX));
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
    PaymentType paymentType = request.paymentType() == null ? PaymentType.PIX : request.paymentType();
    Card card = null;
    Account account = null;
    if (paymentType == PaymentType.PIX) {
      if (request.accountId() == null) {
        throw new IllegalArgumentException("conta é obrigatória para PIX");
      }
      if (request.cardId() != null) {
        throw new IllegalArgumentException("cartão deve ser nulo para PIX");
      }
      account = accountRepository.findByIdAndUserId(request.accountId(), userId)
        .orElseThrow(() -> new IllegalArgumentException("conta não encontrada"));
    } else if (paymentType == PaymentType.CARD) {
      if (request.cardId() == null) {
        throw new IllegalArgumentException("cartão é obrigatório para pagamento com cartão");
      }
      if (request.accountId() != null) {
        throw new IllegalArgumentException("conta deve ser nula para cartão");
      }
      card = cardRepository.findByIdAndUserId(request.cardId(), userId)
        .orElseThrow(() -> new IllegalArgumentException("cartão não encontrado"));
      if (!card.isActive()) {
        throw new IllegalArgumentException("cartão desativado");
      }
      if (!Objects.equals(card.getAccount().getUser().getId(), userId)) {
        throw new IllegalArgumentException("cartão não pertence ao usuário");
      }
    }
    validateCategory(userId, request.categoryId());

    txn.setAccount(account);
    txn.setCard(card);
    txn.setPaymentType(paymentType);
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
}
