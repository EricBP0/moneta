package com.moneta.auth;

import com.moneta.auth.AuthDtos.AuthResponse;
import com.moneta.auth.AuthDtos.LoginRequest;
import com.moneta.auth.AuthDtos.RefreshRequest;
import com.moneta.auth.AuthDtos.RegisterRequest;
import com.moneta.auth.AuthDtos.UserResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final long refreshTokenTtlDays;

  public AuthService(
    UserRepository userRepository,
    RefreshTokenRepository refreshTokenRepository,
    PasswordEncoder passwordEncoder,
    JwtService jwtService,
    @Value("${app.jwt.refresh-token-ttl-days}") long refreshTokenTtlDays
  ) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.refreshTokenTtlDays = refreshTokenTtlDays;
  }

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    Optional<User> existing = userRepository.findByEmail(request.email());
    if (existing.isPresent()) {
      throw new IllegalArgumentException("email já cadastrado");
    }
    User user = new User();
    user.setEmail(request.email());
    user.setName(request.name());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    userRepository.save(user);
    return issueTokens(user);
  }

  @Transactional
  public AuthResponse login(LoginRequest request) {
    User user = userRepository.findByEmail(request.email())
      .orElseThrow(() -> new IllegalArgumentException("credenciais inválidas"));
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new IllegalArgumentException("credenciais inválidas");
    }
    return issueTokens(user);
  }

  @Transactional
  public AuthResponse refresh(RefreshRequest request) {
    String hashed = hashToken(request.refreshToken());
    RefreshToken token = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hashed)
      .orElseThrow(() -> new IllegalArgumentException("refresh token inválido"));
    if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
      token.setRevokedAt(OffsetDateTime.now());
      throw new IllegalArgumentException("refresh token expirado");
    }
    token.setRevokedAt(OffsetDateTime.now());
    refreshTokenRepository.save(token);
    return issueTokens(token.getUser());
  }

  private AuthResponse issueTokens(User user) {
    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = UUID.randomUUID().toString();
    RefreshToken entity = new RefreshToken();
    entity.setUser(user);
    entity.setTokenHash(hashToken(refreshToken));
    entity.setExpiresAt(OffsetDateTime.now().plusDays(refreshTokenTtlDays));
    refreshTokenRepository.save(entity);
    return new AuthResponse(accessToken, refreshToken, new UserResponse(user.getId(), user.getEmail(), user.getName()));
  }

  private String hashToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashed);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 indisponível", ex);
    }
  }
}
