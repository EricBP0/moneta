package com.moneta.importer;

import com.moneta.auth.User;
import com.moneta.card.PaymentType;
import com.moneta.txn.TxnDirection;
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
@Table(name = "import_row")
public class ImportRow {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "batch_id", nullable = false)
  private ImportBatch batch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "row_index", nullable = false)
  private int rowIndex;

  @Column(name = "raw_line")
  private String rawLine;

  @Column(name = "parsed_date")
  private LocalDate parsedDate;

  @Column
  private String description;

  @Column(name = "amount_cents")
  private Long amountCents;

  @Enumerated(EnumType.STRING)
  @Column
  private TxnDirection direction;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_type", nullable = false)
  private PaymentType paymentType = PaymentType.PIX;

  @Column(name = "parsed_account_name")
  private String parsedAccountName;

  @Column(name = "parsed_card_name")
  private String parsedCardName;

  @Column(name = "resolved_category_id")
  private Long resolvedCategoryId;

  @Column(name = "resolved_subcategory_id")
  private Long resolvedSubcategoryId;

  @Column(name = "resolved_account_id")
  private Long resolvedAccountId;

  @Column(name = "resolved_card_id")
  private Long resolvedCardId;

  @Column
  private String hash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ImportRowStatus status = ImportRowStatus.PARSED;

  @Column(name = "error_message")
  private String errorMessage;

  @Column(name = "created_txn_id")
  private Long createdTxnId;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt = OffsetDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt = OffsetDateTime.now();

  public Long getId() {
    return id;
  }

  public ImportBatch getBatch() {
    return batch;
  }

  public void setBatch(ImportBatch batch) {
    this.batch = batch;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public int getRowIndex() {
    return rowIndex;
  }

  public void setRowIndex(int rowIndex) {
    this.rowIndex = rowIndex;
  }

  public String getRawLine() {
    return rawLine;
  }

  public void setRawLine(String rawLine) {
    this.rawLine = rawLine;
  }

  public LocalDate getParsedDate() {
    return parsedDate;
  }

  public void setParsedDate(LocalDate parsedDate) {
    this.parsedDate = parsedDate;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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

  public PaymentType getPaymentType() {
    return paymentType;
  }

  public void setPaymentType(PaymentType paymentType) {
    this.paymentType = paymentType;
  }

  public String getParsedAccountName() {
    return parsedAccountName;
  }

  public void setParsedAccountName(String parsedAccountName) {
    this.parsedAccountName = parsedAccountName;
  }

  public String getParsedCardName() {
    return parsedCardName;
  }

  public void setParsedCardName(String parsedCardName) {
    this.parsedCardName = parsedCardName;
  }

  public Long getResolvedCategoryId() {
    return resolvedCategoryId;
  }

  public void setResolvedCategoryId(Long resolvedCategoryId) {
    this.resolvedCategoryId = resolvedCategoryId;
  }

  public Long getResolvedSubcategoryId() {
    return resolvedSubcategoryId;
  }

  public void setResolvedSubcategoryId(Long resolvedSubcategoryId) {
    this.resolvedSubcategoryId = resolvedSubcategoryId;
  }

  public Long getResolvedAccountId() {
    return resolvedAccountId;
  }

  public void setResolvedAccountId(Long resolvedAccountId) {
    this.resolvedAccountId = resolvedAccountId;
  }

  public Long getResolvedCardId() {
    return resolvedCardId;
  }

  public void setResolvedCardId(Long resolvedCardId) {
    this.resolvedCardId = resolvedCardId;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public ImportRowStatus getStatus() {
    return status;
  }

  public void setStatus(ImportRowStatus status) {
    this.status = status;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public Long getCreatedTxnId() {
    return createdTxnId;
  }

  public void setCreatedTxnId(Long createdTxnId) {
    this.createdTxnId = createdTxnId;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
