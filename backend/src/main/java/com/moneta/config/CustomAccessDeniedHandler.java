package com.moneta.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Custom access denied handler that returns 403 Forbidden
 * when an authenticated user tries to access a resource they don't have permission for.
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void handle(
    HttpServletRequest request,
    HttpServletResponse response,
    AccessDeniedException accessDeniedException
  ) throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    Map<String, Object> body = new HashMap<>();
    body.put("status", 403);
    body.put("error", "Forbidden");
    body.put("message", "You don't have permission to access this resource");
    body.put("path", request.getRequestURI());

    objectMapper.writeValue(response.getOutputStream(), body);
  }
}
