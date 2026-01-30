package com.moneta.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.moneta.support.WebIntegrationTest;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@Tag("integration")
class BudgetAlertIntegrationTest extends WebIntegrationTest {
  @Test
  void triggersBudgetAlertsOnceAtThresholds() throws Exception {
    String token = registerUser("budget-alert@moneta.test").accessToken();
    Long accountId = createAccount(token);
    Long categoryId = createCategory(token, "Alimentação");

    createBudget(token, categoryId, "2024-08", 10000L);

    createTxn(token, accountId, categoryId, 8000L, "2024-08-05T00:00:00Z");
    JsonNode alertsAfter80 = listAlerts(token, "2024-08");
    assertThat(alertsAfter80).hasSize(1);

    createTxn(token, accountId, categoryId, 2000L, "2024-08-10T00:00:00Z");
    JsonNode alertsAfter100 = listAlerts(token, "2024-08");
    assertThat(alertsAfter100).hasSize(2);

    createTxn(token, accountId, categoryId, 1000L, "2024-08-15T00:00:00Z");
    JsonNode alertsAfterExtra = listAlerts(token, "2024-08");
    assertThat(alertsAfterExtra).hasSize(2);

    Set<String> types = new HashSet<>();
    for (JsonNode alert : alertsAfterExtra) {
      types.add(alert.get("type").asText());
    }
    assertThat(types).contains("BUDGET_80", "BUDGET_100");
  }

  private Long createAccount(String token) throws Exception {
    Map<String, Object> request = Map.of(
      "name", "Conta Budget",
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

  private void createTxn(String token, Long accountId, Long categoryId, long amountCents, String occurredAt)
    throws Exception {
    Map<String, Object> request = Map.of(
      "accountId", accountId,
      "amountCents", amountCents,
      "direction", "OUT",
      "description", "Despesa",
      "occurredAt", occurredAt,
      "categoryId", categoryId
    );
    mockMvc.perform(post("/api/txns")
        .header("Authorization", bearerToken(token))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isOk());
  }

  private JsonNode listAlerts(String token, String month) throws Exception {
    MvcResult result = mockMvc.perform(get("/api/alerts")
        .param("month", month)
        .header("Authorization", bearerToken(token)))
      .andExpect(status().isOk())
      .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }
}
