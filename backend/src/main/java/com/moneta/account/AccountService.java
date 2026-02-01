package com.moneta.account;

import com.moneta.auth.User;
import com.moneta.auth.UserRepository;
import com.moneta.txn.TxnRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
  private final AccountRepository accountRepository;
  private final UserRepository userRepository;
  private final TxnRepository txnRepository;

  public AccountService(
    AccountRepository accountRepository,
    UserRepository userRepository,
    TxnRepository txnRepository
  ) {
    this.accountRepository = accountRepository;
    this.userRepository = userRepository;
    this.txnRepository = txnRepository;
  }

  @Transactional
  public Account create(Long userId, AccountDtos.AccountRequest request) {
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("usuário não encontrado"));
    Account account = new Account();
    account.setUser(user);
    account.setInstitutionId(request.institutionId());
    account.setName(request.name());
    account.setType(request.type());
    account.setCurrency(request.currency());
    account.setInitialBalanceCents(request.initialBalanceCents());
    return accountRepository.save(account);
  }

  public List<Account> list(Long userId) {
    return accountRepository.findAllByUserIdAndIsActiveTrue(userId);
  }

  public List<AccountWithBalance> listWithBalances(Long userId) {
    List<Account> accounts = accountRepository.findAllByUserIdAndIsActiveTrue(userId);
    var balanceMap = txnRepository.findSettledBalancesByUserId(userId).stream()
      .collect(java.util.stream.Collectors.toMap(
        com.moneta.txn.TxnRepository.TxnBalanceProjection::getAccountId,
        com.moneta.txn.TxnRepository.TxnBalanceProjection::getBalanceCents
      ));

    return accounts.stream()
      .map(account -> new AccountWithBalance(
        account,
        account.getInitialBalanceCents() + balanceMap.getOrDefault(account.getId(), 0L)
      ))
      .toList();
  }

  public Account get(Long userId, Long id) {
    return accountRepository.findByIdAndUserId(id, userId)
      .orElseThrow(() -> new IllegalArgumentException("conta não encontrada"));
  }

  public AccountWithBalance getWithBalance(Long userId, Long id) {
    Account account = get(userId, id);
    Long postedBalance = txnRepository.findSettledBalanceByUserIdAndAccountId(userId, id);
    long balance = account.getInitialBalanceCents() + (postedBalance == null ? 0L : postedBalance);
    return new AccountWithBalance(account, balance);
  }

  @Transactional
  public Account update(Long userId, Long id, AccountDtos.AccountRequest request) {
    Account account = get(userId, id);
    account.setInstitutionId(request.institutionId());
    account.setName(request.name());
    account.setType(request.type());
    account.setCurrency(request.currency());
    account.setInitialBalanceCents(request.initialBalanceCents());
    return accountRepository.save(account);
  }

  @Transactional
  public void softDelete(Long userId, Long id) {
    Account account = get(userId, id);
    account.setActive(false);
    accountRepository.save(account);
  }

  public record AccountWithBalance(Account account, Long balanceCents) {}
}
