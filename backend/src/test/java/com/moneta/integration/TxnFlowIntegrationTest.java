package com.moneta.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.moneta.support.WebIntegrationTest;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class TxnFlowIntegrationTest extends WebIntegrationTest {
  @Test
  void createsTxnsFiltersByMonthAndHandlesTransfer() throws Exception {
    String token = registerUser("txn-flow@moneta.test").accessToken();

    Long accountId = createAccount(token, "Carteira", 10000L);
    Long secondAccountId = createAccount(token, "Poupança", 0L);

    createTxn(token, accountId, 2000L, "2024-06-05T00:00:00Z", "Mercado", "OUT");
    createTxn(token, accountId, 5000L, "2024-06-10T00:00:00Z", "Salário", "IN");

    MvcResult listResult = mockMvc.perform(get("/api/txns")
        .param("month", "2024-06")
        .param("accountId", accountId.toString())
        .header("Authorization", bearerToken(token)))
      .andExpect(status().isOk())
      .andReturn();

    JsonNode txns = objectMapper.readTree(listResult.getResponse().getContentAsString());
    assertThat(txns).hasSize(2);

    MvcResult accountsResult = mockMvc.perform(get("/api/accounts")
        .header("Authorization", bearerToken(token)))
      .andExpect(status().isOk())
      .andReturn();

    JsonNode accounts = objectMapper.readTree(accountsResult.getResponse().getContentAsString());
    JsonNode accountNode = findById(accounts, accountId);
    assertThat(accountNode.get("balanceCents").asLong()).isEqualTo(13000L);

    Map<String, Object> transfer = Map.of(
      "fromAccountId", accountId,
      "toAccountId", secondAccountId,
      "amountCents", 1500L,
      "occurredAt", OffsetDateTime.parse("2024-06-12T00:00:00Z").toString(),
      "description", "Transferência"
    );

    MvcResult transferResult = mockMvc.perform(post("/api/txns/transfer")
        .header("Authorization", bearerToken(token))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(transfer)))
      .andExpect(status().isOk())
      .andReturn();

    JsonNode transferJson = objectMapper.readTree(transferResult.getResponse().getContentAsString());
    String transferGroupId = transferJson.get("transferGroupId").asText();
    assertThat(transferGroupId).isNotBlank();
    assertThat(transferJson.get("outgoing").get("transferGroupId").asText()).isEqualTo(transferGroupId);
    assertThat(transferJson.get("incoming").get("transferGroupId").asText()).isEqualTo(transferGroupId);
    assertThat(transferJson.get("outgoing").get("direction").asText()).isEqualTo("OUT");
    assertThat(transferJson.get("incoming").get("direction").asText()).isEqualTo("IN");
  }

  private Long createAccount(String token, String name, long initialBalance) throws Exception {
    Map<String, Object> accountRequest = Map.of(
      "name", name,
      "type", "CHECKING",
      "currency", "BRL",
      "initialBalanceCents", initialBalance
    );

    MvcResult result = mockMvc.perform(post("/api/accounts")
        .header("Authorization", bearerToken(token))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(accountRequest)))
      .andExpect(status().isOk())
      .andReturn();

    JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
    return json.get("id").asLong();
  }

  private void createTxn(
    String token,
    Long accountId,
    long amountCents,
    String occurredAt,
    String description,
    String direction
  ) throws Exception {
    Map<String, Object> request = Map.of(
      "accountId", accountId,
      "amountCents", amountCents,
      "direction", direction,
      "description", description,
      "occurredAt", occurredAt
    );

    mockMvc.perform(post("/api/txns")
        .header("Authorization", bearerToken(token))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isOk());
  }

  private JsonNode findById(JsonNode arrayNode, Long id) {
    for (JsonNode node : arrayNode) {
      if (node.get("id").asLong() == id) {
        return node;
      }
    }
    throw new IllegalStateException("Conta não encontrada");
  }
}
