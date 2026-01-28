package com.moneta.txn;

import com.moneta.account.Account;
import com.moneta.account.AccountRepository;
import com.moneta.auth.User;
import com.moneta.auth.UserRepository;
import com.moneta.txn.TxnDtos.TransferRequest;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {
  private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

  private final TxnRepository txnRepository;
  private final AccountRepository accountRepository;
  private final UserRepository userRepository;

  public TransferService(
    TxnRepository txnRepository,
    AccountRepository accountRepository,
    UserRepository userRepository
  ) {
    this.txnRepository = txnRepository;
    this.accountRepository = accountRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public List<Txn> createTransfer(Long userId, TransferRequest request) {
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("usuário não encontrado"));
    Account fromAccount = accountRepository.findByIdAndUserId(request.fromAccountId(), userId)
      .orElseThrow(() -> new IllegalArgumentException("conta origem não encontrada"));
    Account toAccount = accountRepository.findByIdAndUserId(request.toAccountId(), userId)
      .orElseThrow(() -> new IllegalArgumentException("conta destino não encontrada"));

    UUID transferGroupId = UUID.randomUUID();
    String monthRef = request.occurredAt().format(MONTH_FORMATTER);

    Txn outgoing = buildTransferTxn(
      user,
      fromAccount,
      TxnDirection.OUT,
      request,
      transferGroupId,
      monthRef
    );
    Txn incoming = buildTransferTxn(
      user,
      toAccount,
      TxnDirection.IN,
      request,
      transferGroupId,
      monthRef
    );

    return txnRepository.saveAll(List.of(outgoing, incoming));
  }

  private Txn buildTransferTxn(
    User user,
    Account account,
    TxnDirection direction,
    TransferRequest request,
    UUID transferGroupId,
    String monthRef
  ) {
    Txn txn = new Txn();
    txn.setUser(user);
    txn.setAccount(account);
    txn.setAmountCents(request.amountCents());
    txn.setDirection(direction);
    txn.setDescription(request.description());
    txn.setOccurredAt(request.occurredAt());
    txn.setMonthRef(monthRef);
    txn.setStatus(TxnStatus.POSTED);
    txn.setTxnType(TxnType.TRANSFER);
    txn.setTransferGroupId(transferGroupId);
    txn.setCategorizationMode(TxnCategorizationMode.MANUAL);
    return txn;
  }
}
