package com.moneta.alert;

import com.moneta.auth.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "alerts")
public class Alert {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AlertType type;

  @Column(name = "month_ref", nullable = false)
  private String monthRef;

  @Column(name = "budget_id")
  private Long budgetId;

  @Column(name = "goal_id")
  private Long goalId;

  @Column(nullable = false)
  private String message;

  @Column(name = "is_read", nullable = false)
  private boolean isRead = false;

  @Column(name = "triggered_at", nullable = false)
  private OffsetDateTime triggeredAt = OffsetDateTime.now();

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt = OffsetDateTime.now();

  public Long getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public AlertType getType() {
    return type;
  }

  public void setType(AlertType type) {
    this.type = type;
  }

  public String getMonthRef() {
    return monthRef;
  }

  public void setMonthRef(String monthRef) {
    this.monthRef = monthRef;
  }

  public Long getBudgetId() {
    return budgetId;
  }

  public void setBudgetId(Long budgetId) {
    this.budgetId = budgetId;
  }

  public Long getGoalId() {
    return goalId;
  }

  public void setGoalId(Long goalId) {
    this.goalId = goalId;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public boolean isRead() {
    return isRead;
  }

  public void setRead(boolean read) {
    isRead = read;
  }

  public OffsetDateTime getTriggeredAt() {
    return triggeredAt;
  }

  public void setTriggeredAt(OffsetDateTime triggeredAt) {
    this.triggeredAt = triggeredAt;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
