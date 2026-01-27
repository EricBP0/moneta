package com.moneta.txn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.moneta.account.Account;
import com.moneta.account.AccountRepository;
import com.moneta.auth.User;
import com.moneta.auth.UserRepository;
import com.moneta.txn.TxnDtos.TransferRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {
  @Mock
  private TxnRepository txnRepository;

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private UserRepository userRepository;

  private TransferService transferService;

  @BeforeEach
  void setup() {
    transferService = new TransferService(txnRepository, accountRepository, userRepository);
  }

  @Test
  void createsOutgoingAndIncomingTxns() {
    User user = new User();
    Account fromAccount = new Account();
    Account toAccount = new Account();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(accountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(fromAccount));
    when(accountRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(toAccount));
    when(txnRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

    TransferRequest request = new TransferRequest(
      10L,
      20L,
      750L,
      OffsetDateTime.parse("2024-08-20T10:15:30Z"),
      "Transferência"
    );

    List<Txn> result = transferService.createTransfer(1L, request);

    assertThat(result).hasSize(2);
    Txn outgoing = result.stream().filter(txn -> txn.getDirection() == TxnDirection.OUT).findFirst().orElseThrow();
    Txn incoming = result.stream().filter(txn -> txn.getDirection() == TxnDirection.IN).findFirst().orElseThrow();
    assertThat(outgoing.getTransferGroupId()).isEqualTo(incoming.getTransferGroupId());
    assertThat(outgoing.getTxnType()).isEqualTo(TxnType.TRANSFER);
    assertThat(incoming.getTxnType()).isEqualTo(TxnType.TRANSFER);
    assertThat(outgoing.getStatus()).isEqualTo(TxnStatus.POSTED);
    assertThat(incoming.getStatus()).isEqualTo(TxnStatus.POSTED);
  }
}
