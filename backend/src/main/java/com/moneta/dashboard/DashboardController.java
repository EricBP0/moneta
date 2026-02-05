package com.moneta.dashboard;

import com.moneta.config.UserPrincipal;
import com.moneta.dashboard.DashboardDtos.MonthlyResponse;
import com.moneta.dashboard.DashboardDtos.WidgetConfigDto;
import com.moneta.dashboard.DashboardDtos.WidgetConfigUpdateRequest;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
  private final DashboardService dashboardService;
  private final DashboardWidgetService widgetService;

  public DashboardController(
    DashboardService dashboardService,
    DashboardWidgetService widgetService
  ) {
    this.dashboardService = dashboardService;
    this.widgetService = widgetService;
  }

  @GetMapping("/monthly")
  public MonthlyResponse monthly(
    @AuthenticationPrincipal UserPrincipal principal,
    @RequestParam String month
  ) {
    return dashboardService.getMonthly(principal.getId(), month);
  }

  @GetMapping("/widgets")
  public List<WidgetConfigDto> getWidgets(
    @AuthenticationPrincipal UserPrincipal principal
  ) {
    return widgetService.getWidgetConfig(principal.getId());
  }

  @PutMapping("/widgets")
  public List<WidgetConfigDto> updateWidgets(
    @AuthenticationPrincipal UserPrincipal principal,
    @RequestBody WidgetConfigUpdateRequest request
  ) {
    return widgetService.updateWidgetConfig(principal.getId(), request);
  }
}
