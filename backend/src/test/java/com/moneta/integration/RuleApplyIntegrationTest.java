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
class RuleApplyIntegrationTest extends WebIntegrationTest {
  @Test
  void applyRulesDoesNotOverrideManualCategorization() throws Exception {
    String token = registerUser("rule-apply@moneta.test").accessToken();
    Long accountId = createAccount(token);
    Long manualCategory = createCategory(token, "Manual");
    Long ruleCategory = createCategory(token, "Regra");

    createTxn(token, accountId, "Padaria Central", manualCategory, "2024-07-03T00:00:00Z");
    createTxn(token, accountId, "Padaria Central", null, "2024-07-04T00:00:00Z");

    createRule(token, ruleCategory);

    Map<String, Object> applyRequest = Map.of(
      "month", "2024-07",
      "onlyUncategorized", false,
      "overrideManual", false
    );
    MvcResult applyResult = mockMvc.perform(post("/api/rules/apply")
        .header("Authorization", bearerToken(token))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(applyRequest)))
      .andExpect(status().isOk())
      .andReturn();

    JsonNode applyJson = objectMapper.readTree(applyResult.getResponse().getContentAsString());
    assertThat(applyJson.get("updated").asInt()).isEqualTo(1);

    MvcResult txnsResult = mockMvc.perform(get("/api/txns")
        .param("month", "2024-07")
        .header("Authorization", bearerToken(token)))
      .andExpect(status().isOk())
      .andReturn();

    JsonNode txns = objectMapper.readTree(txnsResult.getResponse().getContentAsString());
    assertThat(txns).hasSize(2);

    JsonNode manualTxn = findTxnByCategory(txns, manualCategory);
    assertThat(manualTxn.get("categorizationMode").asText()).isEqualTo("MANUAL");

    JsonNode ruleTxn = findTxnByCategory(txns, ruleCategory);
    assertThat(ruleTxn.get("categorizationMode").asText()).isEqualTo("RULE");
  }

  private Long createAccount(String token) throws Exception {
    Map<String, Object> request = Map.of(
      "name", "Conta Regra",
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

  private void createTxn(String token, Long accountId, String description, Long categoryId, String occurredAt)
    throws Exception {
    Map<String, Object> request = new java.util.HashMap<>();
    request.put("accountId", accountId);
    request.put("amountCents", 1000L);
    request.put("direction", "OUT");
    request.put("description", description);
    request.put("occurredAt", occurredAt);
    if (categoryId != null) {
      request.put("categoryId", categoryId);
    }
    mockMvc.perform(post("/api/txns")
        .header("Authorization", bearerToken(token))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isOk());
  }

  private void createRule(String token, Long categoryId) throws Exception {
    Map<String, Object> request = Map.of(
      "name", "Regra Padaria",
      "priority", 0,
      "matchType", "CONTAINS",
      "pattern", "Padaria",
      "categoryId", categoryId,
      "isActive", true
    );
    mockMvc.perform(post("/api/rules")
        .header("Authorization", bearerToken(token))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isOk());
  }

  private JsonNode findTxnByCategory(JsonNode txns, Long categoryId) {
    for (JsonNode node : txns) {
      if (node.hasNonNull("categoryId") && node.get("categoryId").asLong() == categoryId) {
        return node;
      }
    }
    throw new IllegalStateException("Transação não encontrada");
  }
}
