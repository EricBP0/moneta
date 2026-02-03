package com.moneta.card;

import com.moneta.account.Account;
import com.moneta.auth.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "card")
public class Card {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @Column(nullable = false)
  private String name;

  @Column
  private String brand;

  @Column(length = 4)
  private String last4;

  @Column(name = "limit_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal limitAmount = BigDecimal.ZERO;

  @Column(name = "closing_day", nullable = false)
  private Integer closingDay;

  @Column(name = "due_day", nullable = false)
  private Integer dueDay;

  @Column(name = "is_active", nullable = false)
  private boolean isActive = true;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt = OffsetDateTime.now();

  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  public Long getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Account getAccount() {
    return account;
  }

  public void setAccount(Account account) {
    this.account = account;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getBrand() {
    return brand;
  }

  public void setBrand(String brand) {
    this.brand = brand;
  }

  public String getLast4() {
    return last4;
  }

  public void setLast4(String last4) {
    this.last4 = last4;
  }

  public BigDecimal getLimitAmount() {
    return limitAmount;
  }

  public void setLimitAmount(BigDecimal limitAmount) {
    this.limitAmount = limitAmount;
  }

  public Integer getClosingDay() {
    return closingDay;
  }

  public void setClosingDay(Integer closingDay) {
    this.closingDay = closingDay;
  }

  public Integer getDueDay() {
    return dueDay;
  }

  public void setDueDay(Integer dueDay) {
    this.dueDay = dueDay;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean active) {
    isActive = active;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
