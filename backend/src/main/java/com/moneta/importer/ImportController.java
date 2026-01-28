package com.moneta.importer;

import com.moneta.config.UserPrincipal;
import com.moneta.importer.ImportDtos.ImportBatchDetailResponse;
import com.moneta.importer.ImportDtos.ImportBatchResponse;
import com.moneta.importer.ImportDtos.ImportCommitRequest;
import com.moneta.importer.ImportDtos.ImportCommitResponse;
import com.moneta.importer.ImportDtos.ImportRowsPageResponse;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/import")
public class ImportController {
  private final ImportService importService;

  public ImportController(ImportService importService) {
    this.importService = importService;
  }

  @PostMapping("/csv")
  public ImportBatchResponse uploadCsv(
    @AuthenticationPrincipal UserPrincipal principal,
    @RequestParam("file") MultipartFile file,
    @RequestParam("accountId") Long accountId
  ) {
    return importService.uploadCsv(principal.getId(), accountId, file);
  }

  @GetMapping("/batches")
  public List<ImportBatchResponse> listBatches(
    @AuthenticationPrincipal UserPrincipal principal
  ) {
    return importService.listBatches(principal.getId());
  }

  @GetMapping("/batches/{id}")
  public ImportBatchDetailResponse getBatch(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id
  ) {
    return importService.getBatch(principal.getId(), id);
  }

  @GetMapping("/batches/{id}/rows")
  public ImportRowsPageResponse listRows(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id,
    @RequestParam(required = false) ImportRowStatus status,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return importService.listRows(principal.getId(), id, status, page, size);
  }

  @PostMapping("/batches/{id}/commit")
  public ImportCommitResponse commit(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id,
    @RequestBody(required = false) ImportCommitRequest request
  ) {
    ImportCommitRequest effectiveRequest = request == null
      ? new ImportCommitRequest(true, true, true)
      : request;
    return importService.commitBatch(principal.getId(), id, effectiveRequest);
  }

  @DeleteMapping("/batches/{id}")
  public void deleteBatch(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id
  ) {
    importService.deleteBatch(principal.getId(), id);
  }
}
