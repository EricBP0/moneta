package com.moneta.account;

import com.moneta.auth.User;
import com.moneta.auth.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
  private final AccountRepository accountRepository;
  private final UserRepository userRepository;

  public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
    this.accountRepository = accountRepository;
    this.userRepository = userRepository;
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

  public Account get(Long userId, Long id) {
    return accountRepository.findByIdAndUserId(id, userId)
      .orElseThrow(() -> new IllegalArgumentException("conta não encontrada"));
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
}
