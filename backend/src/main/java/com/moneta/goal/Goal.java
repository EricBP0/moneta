package com.moneta.goal;

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
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "goals")
public class Goal {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  private String name;

  @Column(name = "target_amount_cents", nullable = false)
  private Long targetAmountCents;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "target_date", nullable = false)
  private LocalDate targetDate;

  @Column(name = "monthly_rate_bps", nullable = false)
  private Integer monthlyRateBps = 0;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private GoalStatus status = GoalStatus.ACTIVE;

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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Long getTargetAmountCents() {
    return targetAmountCents;
  }

  public void setTargetAmountCents(Long targetAmountCents) {
    this.targetAmountCents = targetAmountCents;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getTargetDate() {
    return targetDate;
  }

  public void setTargetDate(LocalDate targetDate) {
    this.targetDate = targetDate;
  }

  public Integer getMonthlyRateBps() {
    return monthlyRateBps;
  }

  public void setMonthlyRateBps(Integer monthlyRateBps) {
    this.monthlyRateBps = monthlyRateBps;
  }

  public GoalStatus getStatus() {
    return status;
  }

  public void setStatus(GoalStatus status) {
    this.status = status;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
