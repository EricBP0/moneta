package com.moneta.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moneta.auth.AuthDtos.LoginRequest;
import com.moneta.auth.AuthDtos.RefreshRequest;
import com.moneta.auth.AuthDtos.RegisterRequest;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
  @Mock
  private UserRepository userRepository;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @Mock
  private JwtService jwtService;

  private PasswordEncoder passwordEncoder;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    passwordEncoder = new BCryptPasswordEncoder();
    authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtService, 30);
  }

  @Test
  void registerCreatesUserAndTokens() {
    RegisterRequest request = new RegisterRequest("test@example.com", "Teste", "senha123");
    when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User user = invocation.getArgument(0);
      User saved = new User();
      saved.setEmail(user.getEmail());
      saved.setName(user.getName());
      saved.setPasswordHash(user.getPasswordHash());
      return saved;
    });
    when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");

    var response = authService.register(request);

    assertThat(response.accessToken()).isEqualTo("access-token");
    assertThat(response.refreshToken()).isNotBlank();
    assertThat(response.user().email()).isEqualTo("test@example.com");
    verify(refreshTokenRepository).save(any(RefreshToken.class));
  }

  @Test
  void loginRejectsInvalidPassword() {
    User user = new User();
    user.setEmail("test@example.com");
    user.setName("Teste");
    user.setPasswordHash(passwordEncoder.encode("senha123"));
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> authService.login(new LoginRequest("test@example.com", "errada")))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void refreshRevokesExistingToken() {
    User user = new User();
    user.setEmail("test@example.com");
    user.setName("Teste");
    when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");

    RefreshToken token = new RefreshToken();
    token.setUser(user);
    token.setTokenHash("hashed");
    token.setExpiresAt(OffsetDateTime.now().plusDays(1));

    when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(any(String.class)))
      .thenReturn(Optional.of(token));

    var response = authService.refresh(new RefreshRequest("raw-token"));

    assertThat(response.accessToken()).isEqualTo("access-token");
    ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(captor.capture());
    assertThat(captor.getAllValues()).anySatisfy(saved -> assertThat(saved.getRevokedAt()).isNotNull());
  }
}
