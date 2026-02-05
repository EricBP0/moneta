package com.moneta.dashboard;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DashboardWidgetConfigRepository extends JpaRepository<DashboardWidgetConfig, Long> {
  List<DashboardWidgetConfig> findAllByUserIdOrderByDisplayOrder(Long userId);
  
  Optional<DashboardWidgetConfig> findByUserIdAndWidgetKey(Long userId, String widgetKey);
  
  void deleteAllByUserId(Long userId);
}
