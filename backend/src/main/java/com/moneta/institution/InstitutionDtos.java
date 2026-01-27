package com.moneta.institution;

import jakarta.validation.constraints.NotBlank;

public class InstitutionDtos {
  public record InstitutionRequest(
    @NotBlank(message = "nome é obrigatório") String name,
    String type
  ) {}

  public record InstitutionResponse(
    Long id,
    String name,
    String type,
    boolean isActive
  ) {}
}
