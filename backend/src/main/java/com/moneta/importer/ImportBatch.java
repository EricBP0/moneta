package com.moneta.importer;

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

@Entity
@Table(name = "import_batch")
public class ImportBatch {
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
  private String filename;

  @Column(name = "uploaded_at", nullable = false)
  private OffsetDateTime uploadedAt = OffsetDateTime.now();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ImportBatchStatus status = ImportBatchStatus.UPLOADED;

  @Column(name = "total_rows", nullable = false)
  private int totalRows;

  @Column(name = "error_rows", nullable = false)
  private int errorRows;

  @Column(name = "duplicate_rows", nullable = false)
  private int duplicateRows;

  @Column(name = "ready_rows", nullable = false)
  private int readyRows;

  @Column(name = "committed_rows", nullable = false)
  private int committedRows;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt = OffsetDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt = OffsetDateTime.now();

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

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public OffsetDateTime getUploadedAt() {
    return uploadedAt;
  }

  public void setUploadedAt(OffsetDateTime uploadedAt) {
    this.uploadedAt = uploadedAt;
  }

  public ImportBatchStatus getStatus() {
    return status;
  }

  public void setStatus(ImportBatchStatus status) {
    this.status = status;
  }

  public int getTotalRows() {
    return totalRows;
  }

  public void setTotalRows(int totalRows) {
    this.totalRows = totalRows;
  }

  public int getErrorRows() {
    return errorRows;
  }

  public void setErrorRows(int errorRows) {
    this.errorRows = errorRows;
  }

  public int getDuplicateRows() {
    return duplicateRows;
  }

  public void setDuplicateRows(int duplicateRows) {
    this.duplicateRows = duplicateRows;
  }

  public int getReadyRows() {
    return readyRows;
  }

  public void setReadyRows(int readyRows) {
    this.readyRows = readyRows;
  }

  public int getCommittedRows() {
    return committedRows;
  }

  public void setCommittedRows(int committedRows) {
    this.committedRows = committedRows;
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
