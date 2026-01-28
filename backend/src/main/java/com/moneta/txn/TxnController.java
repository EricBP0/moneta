package com.moneta.txn;

import com.moneta.config.UserPrincipal;
import com.moneta.txn.TxnDtos.TransferRequest;
import com.moneta.txn.TxnDtos.TransferResponse;
import com.moneta.txn.TxnDtos.TxnFilter;
import com.moneta.txn.TxnDtos.TxnRequest;
import com.moneta.txn.TxnDtos.TxnResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/txns")
public class TxnController {
  private final TxnService txnService;
  private final TransferService transferService;

  public TxnController(TxnService txnService, TransferService transferService) {
    this.txnService = txnService;
    this.transferService = transferService;
  }

  @GetMapping
  public List<TxnResponse> list(
    @AuthenticationPrincipal UserPrincipal principal,
    @RequestParam(required = false) String month,
    @RequestParam(required = false) Long accountId,
    @RequestParam(required = false) Long categoryId,
    @RequestParam(required = false, name = "q") String query,
    @RequestParam(required = false) TxnDirection direction,
    @RequestParam(required = false) TxnStatus status
  ) {
    TxnFilter filter = new TxnFilter(month, accountId, categoryId, query, direction, status);
    return txnService.list(principal.getId(), filter).stream()
      .map(this::toResponse)
      .toList();
  }

  @PostMapping
  public TxnResponse create(
    @AuthenticationPrincipal UserPrincipal principal,
    @Valid @RequestBody TxnRequest request
  ) {
    return toResponse(txnService.create(principal.getId(), request));
  }

  @PatchMapping("/{id}")
  public TxnResponse update(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id,
    @Valid @RequestBody TxnRequest request
  ) {
    return toResponse(txnService.update(principal.getId(), id, request));
  }

  @DeleteMapping("/{id}")
  public void delete(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id
  ) {
    txnService.softDelete(principal.getId(), id);
  }

  @PostMapping("/transfer")
  public TransferResponse createTransfer(
    @AuthenticationPrincipal UserPrincipal principal,
    @Valid @RequestBody TransferRequest request
  ) {
    List<Txn> txns = transferService.createTransfer(principal.getId(), request);
    Txn outgoing = txns.stream()
      .filter(txn -> txn.getDirection() == TxnDirection.OUT)
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("transferência incompleta"));
    Txn incoming = txns.stream()
      .filter(txn -> txn.getDirection() == TxnDirection.IN)
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("transferência incompleta"));

    return new TransferResponse(
      outgoing.getTransferGroupId(),
      toResponse(outgoing),
      toResponse(incoming)
    );
  }

  private TxnResponse toResponse(Txn txn) {
    return new TxnResponse(
      txn.getId(),
      txn.getAccount().getId(),
      txn.getAmountCents(),
      txn.getDirection(),
      txn.getDescription(),
      txn.getOccurredAt(),
      txn.getMonthRef(),
      txn.getStatus(),
      txn.getTxnType(),
      txn.getCategoryId(),
      txn.getSubcategoryId(),
      txn.getRuleId(),
      txn.getImportBatchId(),
      txn.getImportRowId(),
      txn.getCategorizationMode(),
      txn.getTransferGroupId(),
      txn.isActive()
    );
  }
}
