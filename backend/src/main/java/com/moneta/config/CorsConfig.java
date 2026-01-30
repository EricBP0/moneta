package com.moneta.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Central CORS configuration for the application.
 * This configuration allows requests from frontend applications hosted on:
 * - Vercel (*.vercel.app)
 * - Production domains (echomoneta.com.br and www.echomoneta.com.br)
 */
@Configuration
public class CorsConfig {

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    
    // Allow requests from Vercel, production domains, and localhost for development
    configuration.setAllowedOriginPatterns(Arrays.asList(
      "https://*.vercel.app",
      "https://echomoneta.com.br",
      "https://www.echomoneta.com.br",
      "http://localhost:3000",
      "http://localhost:4200",
      "http://localhost:5173"
    ));
    
    // Allow common HTTP methods
    configuration.setAllowedMethods(Arrays.asList(
      "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
    ));
    
    // Allow only specific headers required by the frontend
    configuration.setAllowedHeaders(Arrays.asList(
      "Content-Type",
      "Authorization",
      "Accept"
    ));
    
    // Note: JWT tokens are returned in response body (accessToken, refreshToken fields),
    // not in headers, so no need to expose the Authorization header
    
    // Set to false since we're using JWT tokens (not cookies)
    // If you plan to use cookies for authentication, change this to true
    configuration.setAllowCredentials(false);
    
    // Apply this configuration to all endpoints
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    
    return source;
  }
}
