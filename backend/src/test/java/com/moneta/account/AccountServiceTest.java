package com.moneta.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.moneta.auth.UserRepository;
import com.moneta.account.AccountDtos.AccountRequest;
import com.moneta.txn.TxnRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
  @Mock
  private AccountRepository accountRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private TxnRepository txnRepository;

  private AccountService accountService;

  @BeforeEach
  void setup() {
    accountService = new AccountService(accountRepository, userRepository, txnRepository);
  }

  @Test
  void createRequiresUser() {
    when(userRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> accountService.create(1L, new AccountRequest(null, "Conta", "CHECKING", "BRL", 0L)))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void listReturnsAccounts() {
    Account account = new Account();
    account.setName("Conta");
    when(accountRepository.findAllByUserIdAndIsActiveTrue(1L)).thenReturn(List.of(account));

    var result = accountService.list(1L);

    assertThat(result).hasSize(1);
  }

  @Test
  void listWithBalancesIncludesPostedTxns() {
    Account account = new Account();
    account.setName("Conta");
    account.setInitialBalanceCents(100L);
    setAccountId(account, 10L);
    when(accountRepository.findAllByUserIdAndIsActiveTrue(1L)).thenReturn(List.of(account));

    TxnRepository.TxnBalanceProjection projection = new TxnRepository.TxnBalanceProjection() {
      @Override
      public Long getAccountId() {
        return account.getId();
      }

      @Override
      public Long getBalanceCents() {
        return 250L;
      }
    };
    when(txnRepository.findSettledBalancesByUserId(1L)).thenReturn(List.of(projection));

    var result = accountService.listWithBalances(1L);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().balanceCents()).isEqualTo(350L);
  }

  private void setAccountId(Account account, Long id) {
    try {
      var field = Account.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(account, id);
    } catch (ReflectiveOperationException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Test
  void updatePersistsChanges() {
    Account account = new Account();
    account.setName("Conta");
    when(accountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(account));
    when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

    AccountRequest request = new AccountRequest(2L, "Conta Atualizada", "SAVINGS", "BRL", 100L);
    Account updated = accountService.update(1L, 10L, request);

    assertThat(updated.getName()).isEqualTo("Conta Atualizada");
  }
}
