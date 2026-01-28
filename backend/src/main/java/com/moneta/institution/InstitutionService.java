package com.moneta.institution;

import com.moneta.auth.User;
import com.moneta.auth.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstitutionService {
  private final InstitutionRepository institutionRepository;
  private final UserRepository userRepository;

  public InstitutionService(
    InstitutionRepository institutionRepository,
    UserRepository userRepository
  ) {
    this.institutionRepository = institutionRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public Institution create(Long userId, InstitutionDtos.InstitutionRequest request) {
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("usuário não encontrado"));
    Institution institution = new Institution();
    institution.setUser(user);
    institution.setName(request.name());
    institution.setType(request.type());
    return institutionRepository.save(institution);
  }

  public List<Institution> list(Long userId) {
    return institutionRepository.findAllByUserIdAndIsActiveTrue(userId);
  }

  public Institution get(Long userId, Long id) {
    return institutionRepository.findByIdAndUserId(id, userId)
      .orElseThrow(() -> new IllegalArgumentException("instituição não encontrada"));
  }

  @Transactional
  public Institution update(Long userId, Long id, InstitutionDtos.InstitutionRequest request) {
    Institution institution = get(userId, id);
    institution.setName(request.name());
    institution.setType(request.type());
    return institutionRepository.save(institution);
  }

  @Transactional
  public void softDelete(Long userId, Long id) {
    Institution institution = get(userId, id);
    institution.setActive(false);
    institutionRepository.save(institution);
  }
}
