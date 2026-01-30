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
class GoalFlowIntegrationTest extends WebIntegrationTest {
  @Test
  void tracksGoalProjectionAndAlerts() throws Exception {
    String token = registerUser("goal-flow@moneta.test").accessToken();
    Long goalId = createGoal(token);

    addContribution(token, goalId, "2024-02-01", 500L);

    MvcResult projectionResult = mockMvc.perform(get("/api/goals/{id}/projection", goalId)
        .param("asOf", "2024-02")
        .header("Authorization", bearerToken(token)))
      .andExpect(status().isOk())
      .andReturn();

    JsonNode projectionJson = objectMapper.readTree(projectionResult.getResponse().getContentAsString());
    assertThat(projectionJson.get("monthsRemaining").asInt()).isEqualTo(11);
    assertThat(projectionJson.get("neededMonthlyCents").asLong()).isPositive();

    JsonNode alertsAfterBehind = listAlerts(token, "2024-02");
    assertThat(alertsAfterBehind).hasSize(1);
    assertThat(alertsAfterBehind.get(0).get("type").asText()).isEqualTo("GOAL_BEHIND");

    addContribution(token, goalId, "2024-03-01", 9500L);

    JsonNode alertsAfterReached = listAlerts(token, "2024-03");
    Set<String> alertTypes = new HashSet<>();
    for (JsonNode alert : alertsAfterReached) {
      alertTypes.add(alert.get("type").asText());
    }
    assertThat(alertTypes).contains("GOAL_REACHED");
  }

  private Long createGoal(String token) throws Exception {
    Map<String, Object> request = Map.of(
      "name", "Reserva",
      "targetAmountCents", 10000L,
      "targetDate", "2024-12",
      "startDate", "2024-01-01",
      "monthlyRateBps", 0
    );
    MvcResult result = mockMvc.perform(post("/api/goals")
        .header("Authorization", bearerToken(token))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isOk())
      .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  }

  private void addContribution(String token, Long goalId, String date, long amount) throws Exception {
    Map<String, Object> request = Map.of(
      "contributedAt", date,
      "amountCents", amount,
      "note", "aporte"
    );
    mockMvc.perform(post("/api/goals/{id}/contributions", goalId)
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
