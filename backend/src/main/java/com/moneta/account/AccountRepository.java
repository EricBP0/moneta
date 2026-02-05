package com.moneta.account;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
  List<Account> findAllByUserIdAndIsActiveTrue(Long userId);
  Optional<Account> findByIdAndUserId(Long id, Long userId);
  Optional<Account> findByUserIdAndNameIgnoreCase(Long userId, String name);
}
