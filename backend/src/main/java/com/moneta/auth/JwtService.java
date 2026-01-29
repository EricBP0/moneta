package com.moneta.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final SecretKey secretKey;
  private final long accessTokenTtlMinutes;

  public JwtService(
    @Value("${app.jwt.secret}") String secret,
    @Value("${app.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes
  ) {
    validateSecret(secret);
    this.secretKey = Keys.hmacShaKeyFor(normalizeSecret(secret));
    this.accessTokenTtlMinutes = accessTokenTtlMinutes;
  }

  public String generateAccessToken(User user) {
    Instant now = Instant.now();
    return Jwts.builder()
      .subject(user.getId().toString())
      .claim("email", user.getEmail())
      .issuedAt(Date.from(now))
      .expiration(Date.from(now.plus(accessTokenTtlMinutes, ChronoUnit.MINUTES)))
      .signWith(secretKey)
      .compact();
  }

  public Claims parseClaims(String token) {
    return Jwts.parser()
      .verifyWith(secretKey)
      .build()
      .parseSignedClaims(token)
      .getPayload();
  }

  private byte[] normalizeSecret(String secret) {
    if (secret.length() < 32) {
      String padded = String.format("%1$-32s", secret).replace(' ', '0');
      return padded.getBytes(StandardCharsets.UTF_8);
    }
    return secret.getBytes(StandardCharsets.UTF_8);
  }

  private void validateSecret(String secret) {
    if (secret == null || secret.trim().isEmpty()) {
      throw new IllegalArgumentException("JWT secret must not be empty");
    }
    if (secret.length() < 32) {
      throw new IllegalArgumentException(
        "JWT secret must be at least 32 characters (256 bits) for secure HS256 signing. Current length: " + secret.length()
      );
    }
    // Check for common weak/default values
    if (secret.equals("dev-secret-change-me") || secret.equals("change-me") || secret.equals("secret")) {
      throw new IllegalArgumentException(
        "JWT secret appears to be a default/weak value. Please use a strong, randomly generated secret."
      );
    }
  }
}
