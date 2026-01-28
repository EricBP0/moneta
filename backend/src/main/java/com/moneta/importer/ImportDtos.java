package com.moneta.importer;

import com.moneta.txn.TxnDirection;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class ImportDtos {
  public record ImportBatchTotals(
    int totalRows,
    int errorRows,
    int duplicateRows,
    int readyRows,
    int committedRows
  ) {}

  public record ImportBatchResponse(
    Long batchId,
    Long accountId,
    String filename,
    OffsetDateTime uploadedAt,
    ImportBatchStatus status,
    ImportBatchTotals totals
  ) {}

  public record ImportBatchDetailResponse(
    Long batchId,
    Long accountId,
    String filename,
    OffsetDateTime uploadedAt,
    ImportBatchStatus status,
    ImportBatchTotals totals
  ) {}

  public record ImportRowResponse(
    Long id,
    int rowIndex,
    LocalDate parsedDate,
    String description,
    Long amountCents,
    TxnDirection direction,
    Long resolvedCategoryId,
    Long resolvedSubcategoryId,
    ImportRowStatus status,
    String errorMessage,
    Long createdTxnId
  ) {}

  public record ImportRowsPageResponse(
    List<ImportRowResponse> rows,
    int page,
    int size,
    long totalElements,
    ImportBatchTotals totals
  ) {}

  public record ImportCommitRequest(
    Boolean applyRulesAfterCommit,
    Boolean skipDuplicates,
    Boolean commitOnlyReady
  ) {}

  public record ImportCommitResponse(
    int createdTxns,
    int duplicates,
    int errors,
    int updated,
    ImportBatchStatus batchStatus
  ) {}
}
