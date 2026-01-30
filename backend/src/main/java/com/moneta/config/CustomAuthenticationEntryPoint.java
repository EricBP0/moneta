package com.moneta.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Custom authentication entry point that returns 401 Unauthorized
 * when a user tries to access a protected resource without proper authentication.
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void commence(
    HttpServletRequest request,
    HttpServletResponse response,
    AuthenticationException authException
  ) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    Map<String, Object> body = new HashMap<>();
    body.put("status", 401);
    body.put("error", "Unauthorized");
    body.put("message", "Authentication required to access this resource");
    body.put("path", request.getRequestURI());

    objectMapper.writeValue(response.getOutputStream(), body);
  }
}
