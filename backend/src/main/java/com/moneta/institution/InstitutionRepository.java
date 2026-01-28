package com.moneta.institution;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionRepository extends JpaRepository<Institution, Long> {
  List<Institution> findAllByUserIdAndIsActiveTrue(Long userId);
  Optional<Institution> findByIdAndUserId(Long id, Long userId);
}
