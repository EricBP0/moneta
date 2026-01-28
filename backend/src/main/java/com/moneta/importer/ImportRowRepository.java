package com.moneta.importer;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportRowRepository extends JpaRepository<ImportRow, Long> {
  Page<ImportRow> findByBatchIdAndUserId(Long batchId, Long userId, Pageable pageable);
  Page<ImportRow> findByBatchIdAndUserIdAndStatus(
    Long batchId,
    Long userId,
    ImportRowStatus status,
    Pageable pageable
  );

  List<ImportRow> findByBatchIdAndUserId(Long batchId, Long userId);
  List<ImportRow> findByBatchIdAndUserIdAndStatus(Long batchId, Long userId, ImportRowStatus status);

  long countByBatchIdAndUserId(Long batchId, Long userId);
  long countByBatchIdAndUserIdAndStatus(Long batchId, Long userId, ImportRowStatus status);
  Optional<ImportRow> findByIdAndUserId(Long id, Long userId);
}
