package com.moneta.txn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moneta.account.Account;
import com.moneta.account.AccountRepository;
import com.moneta.alert.AlertService;
import com.moneta.auth.User;
import com.moneta.auth.UserRepository;
import com.moneta.card.CardRepository;
import com.moneta.category.CategoryRepository;
import com.moneta.txn.TxnDtos.TxnFilter;
import com.moneta.txn.TxnDtos.TxnRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class TxnServiceTest {
  @Mock
  private TxnRepository txnRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private CardRepository cardRepository;

  @Mock
  private CategoryRepository categoryRepository;

  @Mock
  private AlertService alertService;

  private TxnService txnService;

  @BeforeEach
  void setup() {
    txnService = new TxnService(txnRepository, userRepository, accountRepository, cardRepository, categoryRepository, alertService);
  }

  @Test
  void createDefaultsToPostedNormal() {
    User user = new User();
    Account account = new Account();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(accountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(account));
    when(txnRepository.save(any(Txn.class))).thenAnswer(invocation -> invocation.getArgument(0));

    OffsetDateTime occurredAt = OffsetDateTime.parse("2024-08-01T10:15:30Z");
    TxnRequest request = new TxnRequest(
      10L,
      500L,
      TxnDirection.OUT,
      "Mercado",
      occurredAt,
      null,
      null,
      null,
      null,
      null
    );

    Txn result = txnService.create(1L, request);

    assertThat(result.getStatus()).isEqualTo(TxnStatus.POSTED);
    assertThat(result.getTxnType()).isEqualTo(TxnType.NORMAL);
    assertThat(result.getMonthRef()).isEqualTo("2024-08");
  }

  @Test
  void updateReplacesFields() {
    Txn existing = new Txn();
    existing.setStatus(TxnStatus.PENDING);
    when(txnRepository.findByIdAndUserIdAndIsActiveTrue(5L, 1L)).thenReturn(Optional.of(existing));
    when(accountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(new Account()));
    when(txnRepository.save(any(Txn.class))).thenAnswer(invocation -> invocation.getArgument(0));

    OffsetDateTime occurredAt = OffsetDateTime.parse("2024-08-15T10:15:30Z");
    TxnRequest request = new TxnRequest(
      10L,
      300L,
      TxnDirection.IN,
      "Salário",
      occurredAt,
      TxnStatus.CANCELED,
      null,
      null,
      null,
      null
    );

    Txn updated = txnService.update(1L, 5L, request);

    assertThat(updated.getDirection()).isEqualTo(TxnDirection.IN);
    assertThat(updated.getStatus()).isEqualTo(TxnStatus.CANCELED);
    assertThat(updated.getMonthRef()).isEqualTo("2024-08");
  }

  @Test
  void listAppliesFilters() {
    when(txnRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

    TxnFilter filter = new TxnFilter("2024-08", 10L, 20L, "mercado", TxnDirection.OUT, TxnStatus.POSTED);
    txnService.list(1L, filter);

    verify(txnRepository).findAll(any(Specification.class), eq(Sort.by(Sort.Direction.DESC, "occurredAt")));
  }

  @Test
  void updatePreservesStatusWhenNull() {
    Txn existing = new Txn();
    existing.setStatus(TxnStatus.PENDING);
    when(txnRepository.findByIdAndUserIdAndIsActiveTrue(5L, 1L)).thenReturn(Optional.of(existing));
    when(accountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(new Account()));
    when(txnRepository.save(any(Txn.class))).thenAnswer(invocation -> invocation.getArgument(0));

    OffsetDateTime occurredAt = OffsetDateTime.parse("2024-08-15T10:15:30Z");
    TxnRequest request = new TxnRequest(
      10L,
      300L,
      TxnDirection.IN,
      "Salário",
      occurredAt,
      null,
      null,
      null,
      null,
      null
    );

    Txn updated = txnService.update(1L, 5L, request);

    assertThat(updated.getStatus()).isEqualTo(TxnStatus.PENDING);
  }
}
