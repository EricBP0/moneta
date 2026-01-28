package com.moneta.alert;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public class AlertDtos {
  public record AlertResponse(
    Long id,
    AlertType type,
    String message,
    boolean isRead,
    OffsetDateTime triggeredAt
  ) {}

  public record AlertUpdateRequest(
    @NotNull(message = "status é obrigatório") Boolean isRead
  ) {}
}
