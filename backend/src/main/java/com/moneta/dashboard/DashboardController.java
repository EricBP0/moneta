package com.moneta.dashboard;

import com.moneta.config.UserPrincipal;
import com.moneta.dashboard.DashboardDtos.MonthlyResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
  private final DashboardService dashboardService;

  public DashboardController(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @GetMapping("/monthly")
  public MonthlyResponse monthly(
    @AuthenticationPrincipal UserPrincipal principal,
    @RequestParam String month
  ) {
    return dashboardService.getMonthly(principal.getId(), month);
  }
}
