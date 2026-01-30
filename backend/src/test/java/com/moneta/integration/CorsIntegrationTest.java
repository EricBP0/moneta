package com.moneta.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.moneta.support.WebIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class CorsIntegrationTest extends WebIntegrationTest {

  @Test
  void preflightOptionsRequestFromVercelOriginShouldIncludeCorsHeaders() throws Exception {
    MvcResult result = mockMvc.perform(options("/api/auth/register")
        .header(HttpHeaders.ORIGIN, "https://myapp.vercel.app")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
      .andExpect(status().isOk())
      .andReturn();

    assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
      .isEqualTo("https://myapp.vercel.app");
    assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
      .contains("POST");
    assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS))
      .contains("Content-Type");
  }

  @Test
  void preflightOptionsRequestFromProductionDomainShouldIncludeCorsHeaders() throws Exception {
    MvcResult result = mockMvc.perform(options("/api/auth/register")
        .header(HttpHeaders.ORIGIN, "https://echomoneta.com.br")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
      .andExpect(status().isOk())
      .andReturn();

    assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
      .isEqualTo("https://echomoneta.com.br");
    assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
      .contains("POST");
  }

  @Test
  void preflightOptionsRequestFromLocalhostShouldIncludeCorsHeaders() throws Exception {
    MvcResult result = mockMvc.perform(options("/api/auth/register")
        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
      .andExpect(status().isOk())
      .andReturn();

    assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
      .isEqualTo("http://localhost:3000");
  }

  @Test
  void actualPostRequestFromAllowedOriginShouldIncludeCorsHeaders() throws Exception {
    Map<String, String> register = Map.of(
      "email", "cors-test@moneta.test",
      "name", "CORS Test",
      "password", "senha123"
    );

    MvcResult result = mockMvc.perform(post("/api/auth/register")
        .header(HttpHeaders.ORIGIN, "https://myapp.vercel.app")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(register)))
      .andExpect(status().isOk())
      .andReturn();

    assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
      .isEqualTo("https://myapp.vercel.app");
  }

  @Test
  void preflightOptionsRequestFromUnauthorizedOriginShouldReturnForbidden() throws Exception {
    // Spring Security CORS validation rejects preflight requests from unauthorized origins with 403
    MvcResult result = mockMvc.perform(options("/api/auth/register")
        .header(HttpHeaders.ORIGIN, "https://malicious-site.com")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
      .andExpect(status().isForbidden())
      .andReturn();

    // Verify no CORS headers are present
    assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
      .isNull();
  }

  @Test
  void actualPostRequestFromUnauthorizedOriginShouldReturnForbidden() throws Exception {
    Map<String, String> register = Map.of(
      "email", "cors-test-2@moneta.test",
      "name", "CORS Test 2",
      "password", "senha123"
    );

    // Spring Security CORS validation rejects requests from unauthorized origins with 403
    MvcResult result = mockMvc.perform(post("/api/auth/register")
        .header(HttpHeaders.ORIGIN, "https://malicious-site.com")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(register)))
      .andExpect(status().isForbidden())
      .andReturn();

    // Verify no CORS headers are present
    assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
      .isNull();
  }
}
