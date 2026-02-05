package com.moneta.dashboard;

import com.moneta.auth.User;
import com.moneta.auth.UserRepository;
import com.moneta.dashboard.DashboardDtos.WidgetConfigDto;
import com.moneta.dashboard.DashboardDtos.WidgetConfigUpdateRequest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardWidgetService {
  // Define known widget keys
  public static final String WIDGET_SUMMARY = "SUMMARY";
  public static final String WIDGET_BUDGETS = "BUDGETS";
  public static final String WIDGET_GOALS = "GOALS";
  public static final String WIDGET_ALERTS = "ALERTS";
  public static final String WIDGET_CARD_LIMITS = "CARD_LIMITS";

  private static final Set<String> KNOWN_WIDGET_KEYS = new HashSet<>(Arrays.asList(
    WIDGET_SUMMARY,
    WIDGET_BUDGETS,
    WIDGET_GOALS,
    WIDGET_ALERTS,
    WIDGET_CARD_LIMITS
  ));

  private static final List<WidgetConfigDto> DEFAULT_WIDGETS = List.of(
    new WidgetConfigDto(WIDGET_SUMMARY, true, 0, null),
    new WidgetConfigDto(WIDGET_BUDGETS, true, 1, null),
    new WidgetConfigDto(WIDGET_GOALS, true, 2, null),
    new WidgetConfigDto(WIDGET_ALERTS, true, 3, null),
    new WidgetConfigDto(WIDGET_CARD_LIMITS, true, 4, null)
  );

  private final DashboardWidgetConfigRepository repository;
  private final UserRepository userRepository;

  public DashboardWidgetService(
    DashboardWidgetConfigRepository repository,
    UserRepository userRepository
  ) {
    this.repository = repository;
    this.userRepository = userRepository;
  }

  public List<WidgetConfigDto> getWidgetConfig(Long userId) {
    List<DashboardWidgetConfig> configs = repository.findAllByUserIdOrderByDisplayOrder(userId);
    
    // If user has no configuration, return defaults
    if (configs.isEmpty()) {
      return DEFAULT_WIDGETS;
    }

    return configs.stream()
      .map(this::toDto)
      .toList();
  }

  @Transactional
  public List<WidgetConfigDto> updateWidgetConfig(Long userId, WidgetConfigUpdateRequest request) {
    // Validate request
    if (request.widgets() == null || request.widgets().isEmpty()) {
      throw new IllegalArgumentException("widgets list cannot be empty");
    }

    // Validate widget keys
    Set<String> requestKeys = new HashSet<>();
    for (WidgetConfigDto widget : request.widgets()) {
      if (!KNOWN_WIDGET_KEYS.contains(widget.widgetKey())) {
        throw new IllegalArgumentException("unknown widget key: " + widget.widgetKey());
      }
      if (!requestKeys.add(widget.widgetKey())) {
        throw new IllegalArgumentException("duplicate widget key: " + widget.widgetKey());
      }
    }

    // Validate display order consistency
    Set<Integer> orders = request.widgets().stream()
      .map(WidgetConfigDto::displayOrder)
      .collect(Collectors.toSet());
    if (orders.size() != request.widgets().size()) {
      throw new IllegalArgumentException("display_order values must be unique");
    }

    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("user not found"));

    // Get existing configs
    List<DashboardWidgetConfig> existingConfigs = repository.findAllByUserIdOrderByDisplayOrder(userId);
    Map<String, DashboardWidgetConfig> existingMap = existingConfigs.stream()
      .collect(Collectors.toMap(DashboardWidgetConfig::getWidgetKey, config -> config));

    // Update or create configs
    List<DashboardWidgetConfig> configsToSave = new ArrayList<>();
    for (WidgetConfigDto dto : request.widgets()) {
      DashboardWidgetConfig config = existingMap.get(dto.widgetKey());
      if (config == null) {
        config = new DashboardWidgetConfig();
        config.setUser(user);
        config.setWidgetKey(dto.widgetKey());
      }
      config.setEnabled(dto.isEnabled());
      config.setDisplayOrder(dto.displayOrder());
      config.setSettingsJson(dto.settingsJson());
      config.setUpdatedAt(OffsetDateTime.now());
      configsToSave.add(config);
    }
    repository.saveAll(configsToSave);

    // Delete any widgets not in the request
    List<DashboardWidgetConfig> configsToDelete = existingConfigs.stream()
      .filter(config -> !requestKeys.contains(config.getWidgetKey()))
      .toList();
    if (!configsToDelete.isEmpty()) {
      repository.deleteAll(configsToDelete);
    }

    return getWidgetConfig(userId);
  }

  @Transactional
  public void initializeDefaultWidgets(Long userId) {
    List<DashboardWidgetConfig> existing = repository.findAllByUserIdOrderByDisplayOrder(userId);
    if (!existing.isEmpty()) {
      return; // Already initialized
    }

    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("user not found"));

    for (WidgetConfigDto defaultWidget : DEFAULT_WIDGETS) {
      DashboardWidgetConfig config = new DashboardWidgetConfig();
      config.setUser(user);
      config.setWidgetKey(defaultWidget.widgetKey());
      config.setEnabled(defaultWidget.isEnabled());
      config.setDisplayOrder(defaultWidget.displayOrder());
      config.setSettingsJson(defaultWidget.settingsJson());
      repository.save(config);
    }
  }

  private WidgetConfigDto toDto(DashboardWidgetConfig config) {
    return new WidgetConfigDto(
      config.getWidgetKey(),
      config.isEnabled(),
      config.getDisplayOrder(),
      config.getSettingsJson()
    );
  }
}
