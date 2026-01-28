package com.moneta.alert;

import com.moneta.alert.AlertDtos.AlertResponse;
import com.moneta.alert.AlertDtos.AlertUpdateRequest;
import com.moneta.config.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {
  private final AlertService alertService;

  public AlertController(AlertService alertService) {
    this.alertService = alertService;
  }

  @GetMapping
  public List<AlertResponse> list(
    @AuthenticationPrincipal UserPrincipal principal,
    @RequestParam(required = false) String month
  ) {
    return alertService.list(principal.getId(), month).stream()
      .map(this::toResponse)
      .toList();
  }

  @PatchMapping("/{id}")
  public AlertResponse markRead(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id,
    @Valid @RequestBody AlertUpdateRequest request
  ) {
    return toResponse(alertService.markRead(principal.getId(), id, request.isRead()));
  }

  private AlertResponse toResponse(Alert alert) {
    return new AlertResponse(
      alert.getId(),
      alert.getType(),
      alert.getMessage(),
      alert.isRead(),
      alert.getTriggeredAt()
    );
  }
}
