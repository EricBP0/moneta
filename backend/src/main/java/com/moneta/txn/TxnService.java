package com.moneta.txn;

import com.moneta.account.Account;
import com.moneta.account.AccountRepository;
import com.moneta.auth.User;
import com.moneta.auth.UserRepository;
import com.moneta.category.CategoryRepository;
import com.moneta.txn.TxnDtos.TxnFilter;
import com.moneta.txn.TxnDtos.TxnRequest;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TxnService {
  private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

  private final TxnRepository txnRepository;
  private final UserRepository userRepository;
  private final AccountRepository accountRepository;
  private final CategoryRepository categoryRepository;

  public TxnService(
    TxnRepository txnRepository,
    UserRepository userRepository,
    AccountRepository accountRepository,
    CategoryRepository categoryRepository
  ) {
    this.txnRepository = txnRepository;
    this.userRepository = userRepository;
    this.accountRepository = accountRepository;
    this.categoryRepository = categoryRepository;
  }

  @Transactional
  public Txn create(Long userId, TxnRequest request) {
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("usuário não encontrado"));
    Account account = accountRepository.findByIdAndUserId(request.accountId(), userId)
      .orElseThrow(() -> new IllegalArgumentException("conta não encontrada"));
    validateCategory(userId, request.categoryId());

    Txn txn = new Txn();
    txn.setUser(user);
    txn.setAccount(account);
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
    txn.setImportBatchId(request.importBatchId());
    return txnRepository.save(txn);
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
    Account account = accountRepository.findByIdAndUserId(request.accountId(), userId)
      .orElseThrow(() -> new IllegalArgumentException("conta não encontrada"));
    validateCategory(userId, request.categoryId());

    txn.setAccount(account);
    txn.setAmountCents(request.amountCents());
    txn.setDirection(request.direction());
    txn.setDescription(request.description());
    txn.setOccurredAt(request.occurredAt());
    txn.setMonthRef(request.occurredAt().format(MONTH_FORMATTER));
    txn.setStatus(request.status() == null ? txn.getStatus() : request.status());
    txn.setCategoryId(request.categoryId());
    txn.setSubcategoryId(request.subcategoryId());
    txn.setRuleId(request.ruleId());
    txn.setImportBatchId(request.importBatchId());
    return txnRepository.save(txn);
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
}
