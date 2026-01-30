package com.moneta.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.moneta.support.WebIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@Tag("integration")
class AuthFlowIntegrationTest extends WebIntegrationTest {
  @Test
  void registerLoginRefreshAndMe() throws Exception {
    Map<String, String> register = Map.of(
      "email", "auth-flow@moneta.test",
      "name", "Auth Flow",
      "password", "senha123"
    );

    MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(register)))
      .andExpect(status().isOk())
      .andReturn();

    JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
    assertThat(registerJson.get("accessToken").asText()).isNotBlank();
    assertThat(registerJson.get("refreshToken").asText()).isNotBlank();
    assertThat(registerJson.get("user").get("email").asText()).isEqualTo("auth-flow@moneta.test");

    Map<String, String> login = Map.of(
      "email", "auth-flow@moneta.test",
      "password", "senha123"
    );

    MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(login)))
      .andExpect(status().isOk())
      .andReturn();

    JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
    String accessToken = loginJson.get("accessToken").asText();
    String refreshToken = loginJson.get("refreshToken").asText();
    assertThat(accessToken).isNotBlank();
    assertThat(refreshToken).isNotBlank();

    Map<String, String> refresh = Map.of("refreshToken", refreshToken);
    MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(refresh)))
      .andExpect(status().isOk())
      .andReturn();

    JsonNode refreshJson = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
    String refreshedAccessToken = refreshJson.get("accessToken").asText();
    assertThat(refreshedAccessToken).isNotBlank();

    MvcResult meResult = mockMvc.perform(get("/api/me")
        .header("Authorization", bearerToken(refreshedAccessToken)))
      .andExpect(status().isOk())
      .andReturn();

    JsonNode meJson = objectMapper.readTree(meResult.getResponse().getContentAsString());
    assertThat(meJson.get("email").asText()).isEqualTo("auth-flow@moneta.test");
    assertThat(meJson.get("name").asText()).isEqualTo("Auth Flow");
  }
}
