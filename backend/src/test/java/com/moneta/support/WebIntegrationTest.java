package com.moneta.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneta.auth.AuthDtos;
import com.moneta.auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
public abstract class WebIntegrationTest extends PostgresContainerTest {
  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  protected ObjectMapper objectMapper;

  @Autowired
  protected AuthService authService;

  protected AuthDtos.AuthResponse registerUser(String email) {
    return authService.register(new AuthDtos.RegisterRequest(email, "Teste", "senha123"));
  }

  protected String bearerToken(String token) {
    return "Bearer " + token;
  }
}
