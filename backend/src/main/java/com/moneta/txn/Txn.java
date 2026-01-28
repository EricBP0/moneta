package com.moneta.txn;

import com.moneta.account.Account;
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
import java.util.UUID;

@Entity
@Table(name = "txn")
public class Txn {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @Column(name = "amount_cents", nullable = false)
  private Long amountCents;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TxnDirection direction;

  @Column
  private String description;

  @Column(name = "occurred_at", nullable = false)
  private OffsetDateTime occurredAt;

  @Column(name = "month_ref", nullable = false)
  private String monthRef;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TxnStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "txn_type", nullable = false)
  private TxnType txnType;

  @Column(name = "category_id")
  private Long categoryId;

  @Column(name = "subcategory_id")
  private Long subcategoryId;

  @Column(name = "rule_id")
  private Long ruleId;

  @Enumerated(EnumType.STRING)
  @Column(name = "categorization_mode")
  private TxnCategorizationMode categorizationMode;

  @Column(name = "import_batch_id")
  private Long importBatchId;

  @Column(name = "transfer_group_id")
  private UUID transferGroupId;

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

  public Account getAccount() {
    return account;
  }

  public void setAccount(Account account) {
    this.account = account;
  }

  public Long getAmountCents() {
    return amountCents;
  }

  public void setAmountCents(Long amountCents) {
    this.amountCents = amountCents;
  }

  public TxnDirection getDirection() {
    return direction;
  }

  public void setDirection(TxnDirection direction) {
    this.direction = direction;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public OffsetDateTime getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(OffsetDateTime occurredAt) {
    this.occurredAt = occurredAt;
  }

  public String getMonthRef() {
    return monthRef;
  }

  public void setMonthRef(String monthRef) {
    this.monthRef = monthRef;
  }

  public TxnStatus getStatus() {
    return status;
  }

  public void setStatus(TxnStatus status) {
    this.status = status;
  }

  public TxnType getTxnType() {
    return txnType;
  }

  public void setTxnType(TxnType txnType) {
    this.txnType = txnType;
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

  public Long getRuleId() {
    return ruleId;
  }

  public void setRuleId(Long ruleId) {
    this.ruleId = ruleId;
  }

  public TxnCategorizationMode getCategorizationMode() {
    return categorizationMode;
  }

  public void setCategorizationMode(TxnCategorizationMode categorizationMode) {
    this.categorizationMode = categorizationMode;
  }

  public Long getImportBatchId() {
    return importBatchId;
  }

  public void setImportBatchId(Long importBatchId) {
    this.importBatchId = importBatchId;
  }

  public UUID getTransferGroupId() {
    return transferGroupId;
  }

  public void setTransferGroupId(UUID transferGroupId) {
    this.transferGroupId = transferGroupId;
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
