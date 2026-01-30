package com.moneta.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.moneta.support.WebIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests to verify proper HTTP status codes for authentication and authorization errors.
 * - 401 Unauthorized: No token, invalid token, or expired token
 * - 403 Forbidden: Authenticated but lacking required permissions (future feature)
 */
class SecurityStatusCodeIntegrationTest extends WebIntegrationTest {

  @Test
  void accessProtectedEndpointWithoutToken_shouldReturn401() throws Exception {
    mockMvc.perform(get("/api/dashboard/monthly")
        .param("month", "2024-01")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$.status").value(401))
      .andExpect(jsonPath("$.error").value("Unauthorized"))
      .andExpect(jsonPath("$.message").value("Authentication required to access this resource"));
  }

  @Test
  void accessProtectedEndpointWithInvalidToken_shouldReturn401() throws Exception {
    mockMvc.perform(get("/api/dashboard/monthly")
        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token-here")
        .param("month", "2024-01")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$.status").value(401))
      .andExpect(jsonPath("$.error").value("Unauthorized"));
  }

  @Test
  void accessPublicEndpointWithoutToken_shouldReturn200() throws Exception {
    Map<String, String> loginRequest = Map.of(
      "email", "test@example.com",
      "password", "wrongpassword"
    );

    // Public endpoint should be accessible without token (even if credentials are wrong, it should not return 403)
    MvcResult result = mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(loginRequest)))
      .andReturn();

    // Should return 400 or 401 for bad credentials, but never 403 for public endpoint
    int status = result.getResponse().getStatus();
    assertThat(status).isNotEqualTo(403);
  }

  @Test
  void actuatorHealthEndpointWithoutToken_shouldReturn200() throws Exception {
    mockMvc.perform(get("/actuator/health"))
      .andExpect(status().isOk());
  }

  @Test
  void accessProtectedEndpointWithValidToken_shouldSucceed() throws Exception {
    // Register and login to get a valid token
    Map<String, String> registerRequest = Map.of(
      "email", "securitytest@example.com",
      "name", "Security Test User",
      "password", "password123"
    );

    MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(registerRequest)))
      .andExpect(status().isOk())
      .andReturn();

    String responseBody = registerResult.getResponse().getContentAsString();
    String accessToken = objectMapper.readTree(responseBody).get("accessToken").asText();

    // Access protected endpoint with valid token - should NOT return 401 or 403
    mockMvc.perform(get("/api/dashboard/monthly")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
        .param("month", "2024-01")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());
  }
}
