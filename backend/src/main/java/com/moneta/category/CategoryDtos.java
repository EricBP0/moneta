package com.moneta.category;

import jakarta.validation.constraints.NotBlank;

public class CategoryDtos {
  public record CategoryRequest(
    @NotBlank(message = "nome é obrigatório") String name,
    String color
  ) {}

  public record CategoryResponse(
    Long id,
    String name,
    String color,
    boolean isActive
  ) {}
}
