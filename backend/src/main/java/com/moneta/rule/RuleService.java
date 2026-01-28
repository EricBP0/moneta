package com.moneta.rule;

import com.moneta.account.Account;
import com.moneta.account.AccountRepository;
import com.moneta.auth.User;
import com.moneta.auth.UserRepository;
import com.moneta.category.CategoryRepository;
import com.moneta.common.MonthRefValidator;
import com.moneta.rule.RuleDtos.RuleApplyDetail;
import com.moneta.rule.RuleDtos.RuleApplyRequest;
import com.moneta.rule.RuleDtos.RuleApplyResponse;
import com.moneta.rule.RuleDtos.RuleRequest;
import com.moneta.txn.Txn;
import com.moneta.txn.TxnCategorizationMode;
import com.moneta.txn.TxnRepository;
import com.moneta.txn.TxnStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;

@Service
public class RuleService {
  private final RuleRepository ruleRepository;
  private final UserRepository userRepository;
  private final CategoryRepository categoryRepository;
  private final AccountRepository accountRepository;
  private final TxnRepository txnRepository;

  public RuleService(
    RuleRepository ruleRepository,
    UserRepository userRepository,
    CategoryRepository categoryRepository,
    AccountRepository accountRepository,
    TxnRepository txnRepository
  ) {
    this.ruleRepository = ruleRepository;
    this.userRepository = userRepository;
    this.categoryRepository = categoryRepository;
    this.accountRepository = accountRepository;
    this.txnRepository = txnRepository;
  }

  @Transactional
  public Rule create(Long userId, RuleRequest request) {
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("usuário não encontrado"));
    validateCategory(userId, request.categoryId());
    Account account = validateAccount(userId, request.accountId());
    validateRegex(request.matchType(), request.pattern());

    Rule rule = new Rule();
    rule.setUser(user);
    rule.setName(request.name());
    rule.setPriority(request.priority());
    rule.setMatchType(request.matchType());
    rule.setPattern(request.pattern());
    rule.setCategoryId(request.categoryId());
    rule.setSubcategoryId(request.subcategoryId());
    rule.setAccount(account);
    rule.setActive(request.isActive() == null || request.isActive());
    return ruleRepository.save(rule);
  }

  public List<Rule> list(Long userId) {
    return ruleRepository.findAllByUserIdAndIsActiveTrueOrderByPriorityAsc(userId);
  }

  public Rule get(Long userId, Long id) {
    return ruleRepository.findByIdAndUserId(id, userId)
      .orElseThrow(() -> new IllegalArgumentException("regra não encontrada"));
  }

  @Transactional
  public Rule update(Long userId, Long id, RuleRequest request) {
    Rule rule = get(userId, id);
    validateCategory(userId, request.categoryId());
    Account account = validateAccount(userId, request.accountId());
    validateRegex(request.matchType(), request.pattern());

    rule.setName(request.name());
    rule.setPriority(request.priority());
    rule.setMatchType(request.matchType());
    rule.setPattern(request.pattern());
    rule.setCategoryId(request.categoryId());
    rule.setSubcategoryId(request.subcategoryId());
    rule.setAccount(account);
    rule.setActive(request.isActive() == null || request.isActive());
    return ruleRepository.save(rule);
  }

  @Transactional
  public void softDelete(Long userId, Long id) {
    Rule rule = get(userId, id);
    rule.setActive(false);
    ruleRepository.save(rule);
  }

  @Transactional
  public RuleApplyResponse apply(Long userId, RuleApplyRequest request) {
    if (request.month() != null && !request.month().isBlank()) {
      MonthRefValidator.validate(request.month());
    }
    boolean onlyUncategorized = request.onlyUncategorized() == null || request.onlyUncategorized();
    boolean dryRun = request.dryRun() != null && request.dryRun();
    boolean overrideManual = request.overrideManual() != null && request.overrideManual();

    List<Rule> rules = ruleRepository.findAllByUserIdAndIsActiveTrueOrderByPriorityAsc(userId);
    if (rules.isEmpty()) {
      return new RuleApplyResponse(0, 0, 0, List.of());
    }

    Specification<Txn> spec = (root, query, cb) -> {
      List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.get("user").get("id"), userId));
      predicates.add(cb.isTrue(root.get("isActive")));
      predicates.add(cb.equal(root.get("status"), TxnStatus.POSTED));
      if (request.month() != null && !request.month().isBlank()) {
        predicates.add(cb.equal(root.get("monthRef"), request.month()));
      }
      if (request.accountId() != null) {
        predicates.add(cb.equal(root.get("account").get("id"), request.accountId()));
      }
      if (onlyUncategorized) {
        predicates.add(cb.isNull(root.get("categoryId")));
        predicates.add(cb.isNull(root.get("subcategoryId")));
      }
      if (!overrideManual) {
        predicates.add(
          cb.or(
            cb.isNull(root.get("categorizationMode")),
            cb.notEqual(root.get("categorizationMode"), TxnCategorizationMode.MANUAL)
          )
        );
      }
      return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
    };

    List<Txn> txns = txnRepository.findAll(spec);
    int evaluated = txns.size();
    int matched = 0;
    int updated = 0;
    List<RuleApplyDetail> details = new ArrayList<>();
    List<Txn> modifiedTxns = new ArrayList<>();

    for (Txn txn : txns) {
      Rule matchedRule = findFirstMatch(rules, txn);
      if (matchedRule == null) {
        continue;
      }
      matched++;
      if (matchedRule.getCategoryId() != null) {
        txn.setCategoryId(matchedRule.getCategoryId());
      }
      if (matchedRule.getSubcategoryId() != null) {
        txn.setSubcategoryId(matchedRule.getSubcategoryId());
      }
      txn.setRuleId(matchedRule.getId());
      txn.setCategorizationMode(TxnCategorizationMode.RULE);
      modifiedTxns.add(txn);
      if (details.size() < 20) {
        details.add(new RuleApplyDetail(
          txn.getId(),
          matchedRule.getId(),
          txn.getCategoryId(),
          txn.getSubcategoryId()
        ));
      }
    }

    if (!dryRun && !modifiedTxns.isEmpty()) {
      txnRepository.saveAll(modifiedTxns);
      updated = modifiedTxns.size();
    }

    return new RuleApplyResponse(evaluated, matched, updated, details);
  }

  private Rule findFirstMatch(List<Rule> rules, Txn txn) {
    String description = txn.getDescription() == null ? "" : txn.getDescription();
    for (Rule rule : rules) {
      if (rule.getAccount() != null && !Objects.equals(rule.getAccount().getId(), txn.getAccount().getId())) {
        continue;
      }
      if (matches(rule, description)) {
        return rule;
      }
    }
    return null;
  }

  private boolean matches(Rule rule, String description) {
    String target = description.toLowerCase(Locale.ROOT);
    String pattern = rule.getPattern();
    return switch (rule.getMatchType()) {
      case CONTAINS -> target.contains(pattern.toLowerCase(Locale.ROOT));
      case STARTS_WITH -> target.startsWith(pattern.toLowerCase(Locale.ROOT));
      case REGEX -> Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(description).find();
    };
  }

  private void validateCategory(Long userId, Long categoryId) {
    if (categoryId == null) {
      return;
    }
    categoryRepository.findByIdAndUserId(categoryId, userId)
      .orElseThrow(() -> new IllegalArgumentException("categoria não encontrada"));
  }

  private Account validateAccount(Long userId, Long accountId) {
    if (accountId == null) {
      return null;
    }
    return accountRepository.findByIdAndUserId(accountId, userId)
      .orElseThrow(() -> new IllegalArgumentException("conta não encontrada"));
  }

  private void validateRegex(RuleMatchType matchType, String pattern) {
    if (matchType != RuleMatchType.REGEX) {
      return;
    }
    try {
      Pattern.compile(pattern);
    } catch (PatternSyntaxException ex) {
      throw new IllegalArgumentException("regex inválido");
    }
    
  public List<Txn> applyRules(Long userId, List<Txn> txns) {
    return Collections.emptyList();
  }

