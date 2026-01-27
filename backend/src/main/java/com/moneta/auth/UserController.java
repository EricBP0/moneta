package com.moneta.auth;

import com.moneta.auth.AuthDtos.UserResponse;
import com.moneta.config.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {
  private final UserRepository userRepository;

  public UserController(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @GetMapping("/me")
  public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
    User user = userRepository.findById(principal.getId())
      .orElseThrow(() -> new IllegalArgumentException("usuário não encontrado"));
    return new UserResponse(user.getId(), user.getEmail(), user.getName());
  }
}
