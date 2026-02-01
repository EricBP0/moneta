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
class DashboardBudgetIntegrationTest extends WebIntegrationTest {
  @Test
  void includesClearedTransactionsInDashboardAndBudgets() throws Exception {
    String token = registerUser("dashboard-budget@moneta.test").accessToken();
    Long accountId = createAccount(token);
    Long categoryId = createCategory(token, "Mercado");

    createBudget(token, categoryId, "2024-09", 5000L);

    createTxn(token, accountId, categoryId, 2000L, "2024-09-05T00:00:00Z", "OUT", "CLEARED");
    createTxn(token, accountId, null, 7000L, "2024-09-10T00:00:00Z", "IN", "CLEARED");

    MvcResult result = mockMvc.perform(get("/api/dashboard/monthly")
        .param("month", "2024-09")
        .header("Authorization", bearerToken(token)))
      .andExpect(status().isOk())
      .andReturn();

    JsonNode dashboard = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(dashboard.get("incomeCents").asLong()).isEqualTo(7000L);
    assertThat(dashboard.get("expenseCents").asLong()).isEqualTo(2000L);
    assertThat(dashboard.get("netCents").asLong()).isEqualTo(5000L);

    JsonNode budgetStatus = dashboard.get("budgetStatus");
    assertThat(budgetStatus).hasSize(1);
    assertThat(budgetStatus.get(0).get("consumptionCents").asLong()).isEqualTo(2000L);
  }

  private Long createAccount(String token) throws Exception {
    Map<String, Object> request = Map.of(
      "name", "Conta Dashboard",
      "type", "CHECKING",
      "currency", "BRL",
      "initialBalanceCents", 0L
    );
    MvcResult result = mockMvc.perform(post("/api/accounts")
        .header("Authorization", bearerToken(token))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isOk())
      .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  }

  private Long createCategory(String token, String name) throws Exception {
    Map<String, Object> request = Map.of("name", name);
    MvcResult result = mockMvc.perform(post("/api/categories")
        .header("Authorization", bearerToken(token))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isOk())
      .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  }

  private void createBudget(String token, Long categoryId, String monthRef, long limit) throws Exception {
    Map<String, Object> request = Map.of(
      "monthRef", monthRef,
      "categoryId", categoryId,
      "limitCents", limit
    );
    mockMvc.perform(post("/api/budgets")
        .header("Authorization", bearerToken(token))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isOk());
  }

  private void createTxn(
    String token,
    Long accountId,
    Long categoryId,
    long amountCents,
    String occurredAt,
    String direction,
    String status
  ) throws Exception {
    Map<String, Object> request = new java.util.HashMap<>();
    request.put("accountId", accountId);
    request.put("amountCents", amountCents);
    request.put("direction", direction);
    request.put("description", "Movimento");
    request.put("occurredAt", occurredAt);
    request.put("status", status);
    if (categoryId != null) {
      request.put("categoryId", categoryId);
    }
    mockMvc.perform(post("/api/txns")
        .header("Authorization", bearerToken(token))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isOk());
  }
}
