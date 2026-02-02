package com.moneta.importer;

import com.moneta.account.Account;
import com.moneta.account.AccountRepository;
import com.moneta.auth.User;
import com.moneta.auth.UserRepository;
import com.moneta.category.Category;
import com.moneta.category.CategoryRepository;
import com.moneta.importer.CsvParserService.CsvParseResult;
import com.moneta.importer.CsvParserService.CsvParsedRow;
import com.moneta.importer.ImportDtos.ImportBatchDetailResponse;
import com.moneta.importer.ImportDtos.ImportBatchResponse;
import com.moneta.importer.ImportDtos.ImportBatchTotals;
import com.moneta.importer.ImportDtos.ImportCommitRequest;
import com.moneta.importer.ImportDtos.ImportCommitResponse;
import com.moneta.importer.ImportDtos.ImportRowResponse;
import com.moneta.importer.ImportDtos.ImportRowsPageResponse;
import com.moneta.rule.RuleService;
import com.moneta.txn.Txn;
import com.moneta.txn.TxnCategorizationMode;
import com.moneta.txn.TxnDirection;
import com.moneta.txn.TxnRepository;
import com.moneta.txn.TxnStatus;
import com.moneta.txn.TxnType;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportService {
  private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

  private final ImportBatchRepository importBatchRepository;
  private final ImportRowRepository importRowRepository;
  private final UserRepository userRepository;
  private final AccountRepository accountRepository;
  private final CategoryRepository categoryRepository;
  private final TxnRepository txnRepository;
  private final CsvParserService csvParserService;
  private final RuleService ruleService;

  public ImportService(
    ImportBatchRepository importBatchRepository,
    ImportRowRepository importRowRepository,
    UserRepository userRepository,
    AccountRepository accountRepository,
    CategoryRepository categoryRepository,
    TxnRepository txnRepository,
    CsvParserService csvParserService,
    RuleService ruleService
  ) {
    this.importBatchRepository = importBatchRepository;
    this.importRowRepository = importRowRepository;
    this.userRepository = userRepository;
    this.accountRepository = accountRepository;
    this.categoryRepository = categoryRepository;
    this.txnRepository = txnRepository;
    this.csvParserService = csvParserService;
    this.ruleService = ruleService;
  }

  @Transactional
  public ImportBatchResponse uploadCsv(Long userId, Long accountId, MultipartFile file) {
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("usuário não encontrado"));
    Account account = accountRepository.findByIdAndUserId(accountId, userId)
      .orElseThrow(() -> new IllegalArgumentException("conta não encontrada"));

    CsvParseResult parseResult = parseFile(file);

    ImportBatch batch = new ImportBatch();
    batch.setUser(user);
    batch.setAccount(account);
    batch.setFilename(file.getOriginalFilename() == null ? "upload.csv" : file.getOriginalFilename());
    batch.setStatus(ImportBatchStatus.PARSED);
    batch.setUploadedAt(OffsetDateTime.now());
    batch.setUpdatedAt(OffsetDateTime.now());
    importBatchRepository.save(batch);

    Set<String> existingHashes = buildExistingTxnHashes(userId, accountId);
    Set<String> batchHashes = new HashSet<>();

    List<ImportRow> rows = new ArrayList<>();
    for (CsvParsedRow parsedRow : parseResult.rows()) {
      ImportRow row = new ImportRow();
      row.setBatch(batch);
      row.setUser(user);
      row.setRowIndex(parsedRow.rowIndex());
      row.setRawLine(parsedRow.rawLine());
      row.setParsedDate(parsedRow.parsedDate());
      row.setDescription(parsedRow.description());
      row.setAmountCents(parsedRow.amountCents());
      row.setDirection(parsedRow.direction());
      row.setStatus(parsedRow.status());
      row.setErrorMessage(parsedRow.errorMessage());
      row.setUpdatedAt(OffsetDateTime.now());

      if (parsedRow.status() == ImportRowStatus.PARSED) {
        resolveCategory(userId, parsedRow.categoryName()).ifPresent(row::setResolvedCategoryId);
        String hash = buildHash(userId, accountId, parsedRow.parsedDate(), parsedRow.amountCents(),
          parsedRow.direction(), parsedRow.description());
        if (existingHashes.contains(hash) || batchHashes.contains(hash)) {
          row.setStatus(ImportRowStatus.DUPLICATE);
          row.setErrorMessage("duplicado");
          row.setHash(null);
        } else {
          row.setStatus(ImportRowStatus.READY);
          row.setHash(hash);
          batchHashes.add(hash);
        }
      }

      rows.add(row);
    }

    importRowRepository.saveAll(rows);
    updateTotals(batch, userId);
    return toBatchResponse(batch);
  }

  public List<ImportBatchResponse> listBatches(Long userId) {
    return importBatchRepository.findAllByUserIdOrderByUploadedAtDesc(userId).stream()
      .map(this::toBatchResponse)
      .toList();
  }

  public ImportBatchDetailResponse getBatch(Long userId, Long batchId) {
    ImportBatch batch = getBatchEntity(userId, batchId);
    return new ImportBatchDetailResponse(
      batch.getId(),
      batch.getAccount().getId(),
      batch.getFilename(),
      batch.getUploadedAt(),
      batch.getStatus(),
      toTotals(batch)
    );
  }

  public ImportRowsPageResponse listRows(
    Long userId,
    Long batchId,
    ImportRowStatus status,
    int page,
    int size
  ) {
    ImportBatch batch = getBatchEntity(userId, batchId);
    PageRequest pageRequest = PageRequest.of(page, size);
    Page<ImportRow> rows = status == null
      ? importRowRepository.findByBatchIdAndUserId(batchId, userId, pageRequest)
      : importRowRepository.findByBatchIdAndUserIdAndStatus(batchId, userId, status, pageRequest);

    return new ImportRowsPageResponse(
      rows.stream().map(this::toRowResponse).toList(),
      rows.getNumber(),
      rows.getSize(),
      rows.getTotalElements(),
      toTotals(batch)
    );
  }

  @Transactional
  public ImportCommitResponse commitBatch(Long userId, Long batchId, ImportCommitRequest request) {
    ImportBatch batch = getBatchEntity(userId, batchId);

    boolean applyRulesAfterCommit = request.applyRulesAfterCommit() == null || request.applyRulesAfterCommit();
    boolean skipDuplicates = request.skipDuplicates() == null || request.skipDuplicates();
    boolean commitOnlyReady = request.commitOnlyReady() == null || request.commitOnlyReady();

    List<ImportRow> rowsToCommit = commitOnlyReady
      ? importRowRepository.findByBatchIdAndUserIdAndStatus(batchId, userId, ImportRowStatus.READY)
      : importRowRepository.findByBatchIdAndUserId(batchId, userId);

    Set<String> existingHashes = buildExistingTxnHashes(userId, batch.getAccount().getId());
    int createdCount = 0;
    int duplicateCount = 0;
    List<Txn> createdTxns = new ArrayList<>();

    for (ImportRow row : rowsToCommit) {
      if (row.getStatus() != ImportRowStatus.READY) {
        continue;
      }
      String hash = row.getHash();
      if (hash == null) {
        hash = buildHash(userId, batch.getAccount().getId(), row.getParsedDate(), row.getAmountCents(),
          row.getDirection(), row.getDescription());
      }
      if (skipDuplicates && existingHashes.contains(hash)) {
        row.setStatus(ImportRowStatus.DUPLICATE);
        row.setErrorMessage("duplicado");
        row.setHash(null);
        row.setUpdatedAt(OffsetDateTime.now());
        duplicateCount++;
        continue;
      }

      Txn txn = buildTxnFromRow(batch, row);
      Txn savedTxn = txnRepository.save(txn);
      row.setCreatedTxnId(savedTxn.getId());
      row.setStatus(ImportRowStatus.COMMITTED);
      row.setUpdatedAt(OffsetDateTime.now());
      row.setHash(hash);
      createdCount++;
      createdTxns.add(savedTxn);
      existingHashes.add(hash);
    }

    importRowRepository.saveAll(rowsToCommit);

    if (applyRulesAfterCommit && !createdTxns.isEmpty()) {
      List<Txn> updatedTxns = ruleService.applyRules(userId, createdTxns);
      for (Txn txn : updatedTxns) {
        if (txn.getCategorizationMode() != TxnCategorizationMode.MANUAL) {
          txn.setCategorizationMode(TxnCategorizationMode.RULE);
        }
      }
      txnRepository.saveAll(updatedTxns);
    }

    updateTotals(batch, userId);

    int errorCount = (int) importRowRepository.countByBatchIdAndUserIdAndStatus(
      batchId,
      userId,
      ImportRowStatus.ERROR
    );
    int updatedCount = createdCount + duplicateCount;

    return new ImportCommitResponse(
      createdCount,
      duplicateCount,
      errorCount,
      updatedCount,
      batch.getStatus()
    );
  }

  @Transactional
  public void deleteBatch(Long userId, Long batchId) {
    ImportBatch batch = getBatchEntity(userId, batchId);
    if (batch.getStatus() == ImportBatchStatus.COMMITTED) {
      throw new IllegalStateException("batch já comitado");
    }
    importRowRepository.deleteAll(importRowRepository.findByBatchIdAndUserId(batchId, userId));
    importBatchRepository.delete(batch);
  }

  private CsvParseResult parseFile(MultipartFile file) {
    try {
      return csvParserService.parse(file.getInputStream());
    } catch (IOException ex) {
      throw new IllegalStateException("erro ao ler CSV", ex);
    }
  }

  private void updateTotals(ImportBatch batch, Long userId) {
    int totalRows = (int) importRowRepository.countByBatchIdAndUserId(batch.getId(), userId);
    int errorRows = (int) importRowRepository.countByBatchIdAndUserIdAndStatus(
      batch.getId(),
      userId,
      ImportRowStatus.ERROR
    );
    int duplicateRows = (int) importRowRepository.countByBatchIdAndUserIdAndStatus(
      batch.getId(),
      userId,
      ImportRowStatus.DUPLICATE
    );
    int readyRows = (int) importRowRepository.countByBatchIdAndUserIdAndStatus(
      batch.getId(),
      userId,
      ImportRowStatus.READY
    );
    int committedRows = (int) importRowRepository.countByBatchIdAndUserIdAndStatus(
      batch.getId(),
      userId,
      ImportRowStatus.COMMITTED
    );

    batch.setTotalRows(totalRows);
    batch.setErrorRows(errorRows);
    batch.setDuplicateRows(duplicateRows);
    batch.setReadyRows(readyRows);
    batch.setCommittedRows(committedRows);
    batch.setUpdatedAt(OffsetDateTime.now());
    if (batch.getStatus() == ImportBatchStatus.UPLOADED || batch.getStatus() == ImportBatchStatus.PARSED) {
      batch.setStatus(ImportBatchStatus.PARSED);
    }
    if (totalRows > 0 && committedRows > 0 && readyRows == 0 && committedRows + duplicateRows + errorRows == totalRows) {
      batch.setStatus(ImportBatchStatus.COMMITTED);
    }

    importBatchRepository.save(batch);
  }

  private ImportBatch getBatchEntity(Long userId, Long batchId) {
    return importBatchRepository.findByIdAndUserId(batchId, userId)
      .orElseThrow(() -> new IllegalArgumentException("batch não encontrado"));
  }

  private Optional<Long> resolveCategory(Long userId, String name) {
    if (name == null || name.isBlank()) {
      return Optional.empty();
    }
    return categoryRepository.findByUserIdAndNameIgnoreCase(userId, name.trim())
      .map(Category::getId);
  }

  private Set<String> buildExistingTxnHashes(Long userId, Long accountId) {
    List<Txn> txns = txnRepository.findByUserIdAndAccountIdAndIsActiveTrueAndPaymentType(
      userId,
      accountId,
      com.moneta.txn.PaymentType.PIX
    );
    Set<String> hashes = new HashSet<>();
    for (Txn txn : txns) {
      LocalDate date = txn.getOccurredAt().toLocalDate();
      hashes.add(buildHash(
        userId,
        accountId,
        date,
        txn.getAmountCents(),
        txn.getDirection(),
        txn.getDescription()
      ));
    }
    return hashes;
  }

  private String buildHash(
    Long userId,
    Long accountId,
    LocalDate date,
    Long amountCents,
    TxnDirection direction,
    String description
  ) {
    String normalizedDescription = normalizeDescription(description);
    String input = userId + "|" + accountId + "|" + date + "|" + amountCents + "|" + direction + "|"
      + normalizedDescription;
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("hash algorithm missing", ex);
    }
  }

  private String normalizeDescription(String description) {
    if (description == null) {
      return "";
    }
    return description.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
  }

  private Txn buildTxnFromRow(ImportBatch batch, ImportRow row) {
    Txn txn = new Txn();
    txn.setUser(batch.getUser());
    txn.setAccount(batch.getAccount());
    txn.setAmountCents(row.getAmountCents());
    txn.setDirection(row.getDirection());
    txn.setDescription(row.getDescription());
    txn.setOccurredAt(row.getParsedDate().atStartOfDay().atOffset(ZoneOffset.UTC));
    txn.setMonthRef(row.getParsedDate().format(MONTH_FORMATTER));
    txn.setStatus(TxnStatus.POSTED);
    txn.setTxnType(TxnType.NORMAL);
    txn.setCategoryId(row.getResolvedCategoryId());
    txn.setSubcategoryId(row.getResolvedSubcategoryId());
    txn.setImportBatchId(batch.getId());
    txn.setImportRowId(row.getId());
    txn.setCategorizationMode(TxnCategorizationMode.IMPORT);
    return txn;
  }

  private ImportBatchResponse toBatchResponse(ImportBatch batch) {
    return new ImportBatchResponse(
      batch.getId(),
      batch.getAccount().getId(),
      batch.getFilename(),
      batch.getUploadedAt(),
      batch.getStatus(),
      toTotals(batch)
    );
  }

  private ImportBatchTotals toTotals(ImportBatch batch) {
    return new ImportBatchTotals(
      batch.getTotalRows(),
      batch.getErrorRows(),
      batch.getDuplicateRows(),
      batch.getReadyRows(),
      batch.getCommittedRows()
    );
  }

  private ImportRowResponse toRowResponse(ImportRow row) {
    return new ImportRowResponse(
      row.getId(),
      row.getRowIndex(),
      row.getParsedDate(),
      row.getDescription(),
      row.getAmountCents(),
      row.getDirection(),
      row.getResolvedCategoryId(),
      row.getResolvedSubcategoryId(),
      row.getStatus(),
      row.getErrorMessage(),
      row.getCreatedTxnId()
    );
  }
}
