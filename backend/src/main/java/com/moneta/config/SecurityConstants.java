package com.moneta.config;

/**
 * Security-related constants shared across security configuration.
 * Centralizes public path patterns to ensure consistency between SecurityConfig and JWT filter.
 */
public final class SecurityConstants {
  
  private SecurityConstants() {
    // Utility class - no instantiation
  }
  
  /**
   * Path patterns for authentication endpoints (public access)
   */
  public static final String AUTH_PATH_PATTERN = "/api/auth/**";
  
  /**
   * Path patterns for actuator endpoints (public access)
   */
  public static final String ACTUATOR_HEALTH_PATH = "/actuator/health";
  public static final String ACTUATOR_INFO_PATH = "/actuator/info";
  
  /**
   * Path prefix for JWT filter to check if request should skip authentication
   */
  public static final String[] PUBLIC_PATH_PREFIXES = {
    "/api/auth/",
    "/actuator/health",
    "/actuator/info"
  };
}
