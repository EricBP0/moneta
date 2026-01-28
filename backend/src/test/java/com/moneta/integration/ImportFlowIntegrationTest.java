package com.moneta.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.moneta.support.WebIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

class ImportFlowIntegrationTest extends WebIntegrationTest {
  @Test
  void uploadCommitAndApplyRulesOnImport() throws Exception {
    String token = registerUser("import-flow@moneta.test").accessToken();
    Long accountId = createAccount(token);
    Long categoryId = createCategory(token, "Mercado");
    createRule(token, categoryId);

    String csv = "date,description,amount,category\n" +
      "2024-05-10,Super Mercado,50.00,Mercado\n" +
      "2024-05-10,Super Mercado,50.00,Mercado";
    MockMultipartFile file = new MockMultipartFile(
      "file",
      "import.csv",
      "text/csv",
      csv.getBytes(StandardCharsets.UTF_8)
    );

    MvcResult uploadResult = mockMvc.perform(multipart("/api/import/csv")
        .file(file)
        .param("accountId", accountId.toString())
        .header("Authorization", bearerToken(token)))
      .andExpect(status().isOk())
      .andReturn();

    JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
    Long batchId = uploadJson.get("batchId").asLong();
    JsonNode totals = uploadJson.get("totals");
    assertThat(totals.get("totalRows").asInt()).isEqualTo(2);
    assertThat(totals.get("duplicateRows").asInt()).isEqualTo(1);
    assertThat(totals.get("readyRows").asInt()).isEqualTo(1);

    MvcResult rowsResult = mockMvc.perform(get("/api/import/batches/{id}/rows", batchId)
        .header("Authorization", bearerToken(token)))
      .andExpect(status().isOk())
      .andReturn();
    JsonNode rowsJson = objectMapper.readTree(rowsResult.getResponse().getContentAsString());
    assertThat(rowsJson.get("totalElements").asInt()).isEqualTo(2);

    Map<String, Object> commitRequest = Map.of(
      "applyRulesAfterCommit", true,
      "skipDuplicates", true,
      "commitOnlyReady", true
    );
    MvcResult commitResult = mockMvc.perform(post("/api/import/batches/{id}/commit", batchId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(commitRequest))
        .header("Authorization", bearerToken(token)))
      .andExpect(status().isOk())
      .andReturn();

    JsonNode commitJson = objectMapper.readTree(commitResult.getResponse().getContentAsString());
    assertThat(commitJson.get("createdTxns").asInt()).isEqualTo(1);

    MvcResult secondCommit = mockMvc.perform(post("/api/import/batches/{id}/commit", batchId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(commitRequest))
        .header("Authorization", bearerToken(token)))
      .andExpect(status().isOk())
      .andReturn();

    JsonNode secondCommitJson = objectMapper.readTree(secondCommit.getResponse().getContentAsString());
    assertThat(secondCommitJson.get("createdTxns").asInt()).isZero();

    MvcResult txnsResult = mockMvc.perform(get("/api/txns")
        .param("month", "2024-05")
        .header("Authorization", bearerToken(token)))
      .andExpect(status().isOk())
      .andReturn();

    JsonNode txns = objectMapper.readTree(txnsResult.getResponse().getContentAsString());
    assertThat(txns).hasSize(1);
    assertThat(txns.get(0).get("categoryId").asLong()).isEqualTo(categoryId);
    assertThat(txns.get(0).get("categorizationMode").asText()).isEqualTo("RULE");
  }

  private Long createAccount(String token) throws Exception {
    Map<String, Object> accountRequest = Map.of(
      "name", "Conta Import",
      "type", "CHECKING",
      "currency", "BRL",
      "initialBalanceCents", 0L
    );
    MvcResult result = mockMvc.perform(post("/api/accounts")
        .header("Authorization", bearerToken(token))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(accountRequest)))
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

  private void createRule(String token, Long categoryId) throws Exception {
    Map<String, Object> request = Map.of(
      "name", "Regra Mercado",
      "priority", 0,
      "matchType", "CONTAINS",
      "pattern", "Mercado",
      "categoryId", categoryId,
      "isActive", true
    );

    mockMvc.perform(post("/api/rules")
        .header("Authorization", bearerToken(token))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isOk());
  }
}
