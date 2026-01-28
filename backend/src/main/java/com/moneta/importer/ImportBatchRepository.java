package com.moneta.importer;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {
  List<ImportBatch> findAllByUserIdOrderByUploadedAtDesc(Long userId);
  Optional<ImportBatch> findByIdAndUserId(Long id, Long userId);
}
