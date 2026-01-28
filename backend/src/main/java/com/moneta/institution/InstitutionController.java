package com.moneta.institution;

import com.moneta.config.UserPrincipal;
import com.moneta.institution.InstitutionDtos.InstitutionRequest;
import com.moneta.institution.InstitutionDtos.InstitutionResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/institutions")
public class InstitutionController {
  private final InstitutionService institutionService;

  public InstitutionController(InstitutionService institutionService) {
    this.institutionService = institutionService;
  }

  @GetMapping
  public List<InstitutionResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
    return institutionService.list(principal.getId()).stream()
      .map(this::toResponse)
      .toList();
  }

  @PostMapping
  public InstitutionResponse create(
    @AuthenticationPrincipal UserPrincipal principal,
    @Valid @RequestBody InstitutionRequest request
  ) {
    return toResponse(institutionService.create(principal.getId(), request));
  }

  @PatchMapping("/{id}")
  public InstitutionResponse update(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id,
    @Valid @RequestBody InstitutionRequest request
  ) {
    return toResponse(institutionService.update(principal.getId(), id, request));
  }

  @DeleteMapping("/{id}")
  public void delete(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id
  ) {
    institutionService.softDelete(principal.getId(), id);
  }

  private InstitutionResponse toResponse(Institution institution) {
    return new InstitutionResponse(
      institution.getId(),
      institution.getName(),
      institution.getType(),
      institution.isActive()
    );
  }
}
