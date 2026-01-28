package com.moneta.rule;

import com.moneta.config.UserPrincipal;
import com.moneta.rule.RuleDtos.RuleApplyRequest;
import com.moneta.rule.RuleDtos.RuleApplyResponse;
import com.moneta.rule.RuleDtos.RuleRequest;
import com.moneta.rule.RuleDtos.RuleResponse;
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
@RequestMapping("/api/rules")
public class RuleController {
  private final RuleService ruleService;

  public RuleController(RuleService ruleService) {
    this.ruleService = ruleService;
  }

  @GetMapping
  public List<RuleResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
    return ruleService.list(principal.getId()).stream()
      .map(this::toResponse)
      .toList();
  }

  @PostMapping
  public RuleResponse create(
    @AuthenticationPrincipal UserPrincipal principal,
    @Valid @RequestBody RuleRequest request
  ) {
    return toResponse(ruleService.create(principal.getId(), request));
  }

  @PatchMapping("/{id}")
  public RuleResponse update(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id,
    @Valid @RequestBody RuleRequest request
  ) {
    return toResponse(ruleService.update(principal.getId(), id, request));
  }

  @DeleteMapping("/{id}")
  public void delete(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id
  ) {
    ruleService.softDelete(principal.getId(), id);
  }

  @PostMapping("/apply")
  public RuleApplyResponse apply(
    @AuthenticationPrincipal UserPrincipal principal,
    @RequestBody RuleApplyRequest request
  ) {
    return ruleService.apply(principal.getId(), request);
  }

  private RuleResponse toResponse(Rule rule) {
    return new RuleResponse(
      rule.getId(),
      rule.getName(),
      rule.getPriority(),
      rule.getMatchType(),
      rule.getPattern(),
      rule.getCategoryId(),
      rule.getSubcategoryId(),
      rule.getAccount() == null ? null : rule.getAccount().getId(),
      rule.isActive()
    );
  }
}
