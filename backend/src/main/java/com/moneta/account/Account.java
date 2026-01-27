package com.moneta.account;

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
import java.time.OffsetDateTime;

@Entity
@Table(name = "account")
public class Account {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "institution_id")
  private Long institutionId;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String type;

  @Column(nullable = false)
  private String currency = "BRL";

  @Column(name = "initial_balance_cents", nullable = false)
  private Long initialBalanceCents = 0L;

  @Column(name = "is_active", nullable = false)
  private boolean isActive = true;

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

  public Long getInstitutionId() {
    return institutionId;
  }

  public void setInstitutionId(Long institutionId) {
    this.institutionId = institutionId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public Long getInitialBalanceCents() {
    return initialBalanceCents;
  }

  public void setInitialBalanceCents(Long initialBalanceCents) {
    this.initialBalanceCents = initialBalanceCents;
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
}
