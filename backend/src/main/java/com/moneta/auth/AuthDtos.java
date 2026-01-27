package com.moneta.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {
  public record RegisterRequest(
    @Email(message = "email inválido")
    @NotBlank(message = "email é obrigatório")
    String email,
    @NotBlank(message = "nome é obrigatório")
    String name,
    @NotBlank(message = "senha é obrigatória")
    String password
  ) {}

  public record LoginRequest(
    @Email(message = "email inválido")
    @NotBlank(message = "email é obrigatório")
    String email,
    @NotBlank(message = "senha é obrigatória")
    String password
  ) {}

  public record RefreshRequest(
    @NotBlank(message = "refreshToken é obrigatório")
    String refreshToken
  ) {}

  public record AuthResponse(String accessToken, String refreshToken, UserResponse user) {}

  public record UserResponse(Long id, String email, String name) {}
}
