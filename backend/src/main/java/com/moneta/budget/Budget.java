package com.moneta.budget;

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
@Table(name = "budgets")
public class Budget {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "month_ref", nullable = false)
  private String monthRef;

  @Column(name = "category_id")
  private Long categoryId;

  @Column(name = "subcategory_id")
  private Long subcategoryId;

  @Column(name = "limit_cents", nullable = false)
  private Long limitCents;

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

  public String getMonthRef() {
    return monthRef;
  }

  public void setMonthRef(String monthRef) {
    this.monthRef = monthRef;
  }

  public Long getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(Long categoryId) {
    this.categoryId = categoryId;
  }

  public Long getSubcategoryId() {
    return subcategoryId;
  }

  public void setSubcategoryId(Long subcategoryId) {
    this.subcategoryId = subcategoryId;
  }

  public Long getLimitCents() {
    return limitCents;
  }

  public void setLimitCents(Long limitCents) {
    this.limitCents = limitCents;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
