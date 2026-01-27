package com.moneta.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AccountDtos {
  public record AccountRequest(
    Long institutionId,
    @NotBlank(message = "nome é obrigatório") String name,
    @NotBlank(message = "tipo é obrigatório") String type,
    @NotBlank(message = "moeda é obrigatória") String currency,
    @NotNull(message = "saldo inicial é obrigatório") Long initialBalanceCents
  ) {}

  public record AccountResponse(
    Long id,
    Long institutionId,
    String name,
    String type,
    String currency,
    Long initialBalanceCents,
    boolean isActive
  ) {}
}
