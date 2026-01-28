package com.moneta.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moneta.account.Account;
import com.moneta.account.AccountRepository;
import com.moneta.auth.UserRepository;
import com.moneta.category.CategoryRepository;
import com.moneta.rule.RuleDtos.RuleApplyRequest;
import com.moneta.txn.Txn;
import com.moneta.txn.TxnCategorizationMode;
import com.moneta.txn.TxnRepository;
import com.moneta.txn.TxnStatus;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class RuleServiceTest {
  @Mock
  private RuleRepository ruleRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private CategoryRepository categoryRepository;

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private TxnRepository txnRepository;

  private RuleService ruleService;

  @BeforeEach
  void setup() {
    ruleService = new RuleService(ruleRepository, userRepository, categoryRepository, accountRepository, txnRepository);
  }

  @Test
  void applyRespectsPriority() {
    Rule first = new Rule();
    first.setMatchType(RuleMatchType.CONTAINS);
    first.setPattern("super");
    first.setCategoryId(10L);
    Rule second = new Rule();
    second.setMatchType(RuleMatchType.CONTAINS);
    second.setPattern("merc");
    second.setCategoryId(20L);
    when(ruleRepository.findAllByUserIdAndIsActiveTrueOrderByPriorityAsc(1L)).thenReturn(List.of(first, second));

    Txn txn = new Txn();
    txn.setStatus(TxnStatus.POSTED);
    txn.setDescription("Supermercado");
    txn.setAccount(new Account());
    when(txnRepository.findAll(any(Specification.class))).thenReturn(List.of(txn));
    when(txnRepository.saveAll(any())).thenReturn(List.of(txn));

    var response = ruleService.apply(1L, new RuleApplyRequest(null, null, true, false, false));

    assertThat(txn.getCategoryId()).isEqualTo(10L);
    assertThat(response.matched()).isEqualTo(1);
    verify(txnRepository).saveAll(any());
  }

  @Test
  void applySkipsManualWhenNotOverriding() {
    Rule rule = new Rule();
    rule.setMatchType(RuleMatchType.CONTAINS);
    rule.setPattern("uber");
    rule.setCategoryId(30L);
    when(ruleRepository.findAllByUserIdAndIsActiveTrueOrderByPriorityAsc(1L)).thenReturn(List.of(rule));

    // When overrideManual is false, manual txns are filtered at query level
    when(txnRepository.findAll(any(Specification.class))).thenReturn(List.of());

    var response = ruleService.apply(1L, new RuleApplyRequest(null, null, true, false, false));

    assertThat(response.evaluated()).isEqualTo(0);
  }

  @Test
  void applySupportsMatchTypes() {
    Rule contains = new Rule();
    contains.setMatchType(RuleMatchType.CONTAINS);
    contains.setPattern("net");
    contains.setCategoryId(1L);
    Rule startsWith = new Rule();
    startsWith.setMatchType(RuleMatchType.STARTS_WITH);
    startsWith.setPattern("uber");
    startsWith.setCategoryId(2L);
    Rule regex = new Rule();
    regex.setMatchType(RuleMatchType.REGEX);
    regex.setPattern("^123.*");
    regex.setCategoryId(3L);
    when(ruleRepository.findAllByUserIdAndIsActiveTrueOrderByPriorityAsc(1L))
      .thenReturn(List.of(contains, startsWith, regex));

    Txn txn1 = new Txn();
    txn1.setStatus(TxnStatus.POSTED);
    txn1.setDescription("Netflix");
    txn1.setAccount(new Account());
    Txn txn2 = new Txn();
    txn2.setStatus(TxnStatus.POSTED);
    txn2.setDescription("Uber Trip");
    txn2.setAccount(new Account());
    Txn txn3 = new Txn();
    txn3.setStatus(TxnStatus.POSTED);
    txn3.setDescription("123ABC");
    txn3.setAccount(new Account());
    when(txnRepository.findAll(any(Specification.class))).thenReturn(List.of(txn1, txn2, txn3));
    when(txnRepository.saveAll(any())).thenReturn(List.of(txn1, txn2, txn3));

    ruleService.apply(1L, new RuleApplyRequest(null, null, true, false, false));

    assertThat(txn1.getCategoryId()).isEqualTo(1L);
    assertThat(txn2.getCategoryId()).isEqualTo(2L);
    assertThat(txn3.getCategoryId()).isEqualTo(3L);
  }

  @Test
  void applyDryRunDoesNotPersist() {
    Rule rule = new Rule();
    rule.setMatchType(RuleMatchType.CONTAINS);
    rule.setPattern("cafe");
    rule.setCategoryId(10L);
    when(ruleRepository.findAllByUserIdAndIsActiveTrueOrderByPriorityAsc(1L)).thenReturn(List.of(rule));

    Txn txn = new Txn();
    txn.setStatus(TxnStatus.POSTED);
    txn.setDescription("Cafe");
    txn.setAccount(new Account());
    when(txnRepository.findAll(any(Specification.class))).thenReturn(List.of(txn));

    var response = ruleService.apply(1L, new RuleApplyRequest(null, null, true, true, false));

    assertThat(response.updated()).isEqualTo(0);
    verify(txnRepository, never()).saveAll(any());
  }
}
