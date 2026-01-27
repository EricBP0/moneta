package com.moneta.account;

import com.moneta.account.AccountDtos.AccountRequest;
import com.moneta.account.AccountDtos.AccountResponse;
import com.moneta.config.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
  private final AccountService accountService;

  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @GetMapping
  public List<AccountResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
    return accountService.list(principal.getId()).stream()
      .map(this::toResponse)
      .toList();
  }

  @PostMapping
  public AccountResponse create(
    @AuthenticationPrincipal UserPrincipal principal,
    @Valid @RequestBody AccountRequest request
  ) {
    return toResponse(accountService.create(principal.getId(), request));
  }

  @GetMapping("/{id}")
  public AccountResponse get(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id
  ) {
    return toResponse(accountService.get(principal.getId(), id));
  }

  @PatchMapping("/{id}")
  public AccountResponse update(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id,
    @Valid @RequestBody AccountRequest request
  ) {
    return toResponse(accountService.update(principal.getId(), id, request));
  }

  @DeleteMapping("/{id}")
  public void delete(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id
  ) {
    accountService.softDelete(principal.getId(), id);
  }

  private AccountResponse toResponse(Account account) {
    return new AccountResponse(
      account.getId(),
      account.getInstitutionId(),
      account.getName(),
      account.getType(),
      account.getCurrency(),
      account.getInitialBalanceCents(),
      account.isActive()
    );
  }
}
