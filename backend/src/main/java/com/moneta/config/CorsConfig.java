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
    
    // Allow requests from Vercel and production domains
    configuration.setAllowedOriginPatterns(Arrays.asList(
      "https://*.vercel.app",
      "https://echomoneta.com.br",
      "https://www.echomoneta.com.br"
    ));
    
    // Allow common HTTP methods
    configuration.setAllowedMethods(Arrays.asList(
      "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
    ));
    
    // Allow all headers - the frontend can send any header
    configuration.setAllowedHeaders(Arrays.asList("*"));
    
    // Expose Authorization header so frontend can read JWT tokens from responses
    configuration.setExposedHeaders(Arrays.asList("Authorization"));
    
    // Set to false since we're using JWT tokens (not cookies)
    // If you plan to use cookies for authentication, change this to true
    configuration.setAllowCredentials(false);
    
    // Apply this configuration to all endpoints
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    
    return source;
  }
}
