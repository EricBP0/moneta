package com.moneta.institution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.moneta.auth.User;
import com.moneta.auth.UserRepository;
import com.moneta.institution.InstitutionDtos.InstitutionRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InstitutionServiceTest {
  @Mock
  private InstitutionRepository institutionRepository;

  @Mock
  private UserRepository userRepository;

  private InstitutionService institutionService;

  @BeforeEach
  void setup() {
    institutionService = new InstitutionService(institutionRepository, userRepository);
  }

  @Test
  void createRequiresUser() {
    when(userRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> institutionService.create(1L, new InstitutionRequest("Banco XYZ", "BANK")))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void createSavesInstitution() {
    User user = new User();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(institutionRepository.save(any(Institution.class))).thenAnswer(invocation -> invocation.getArgument(0));

    InstitutionRequest request = new InstitutionRequest("Banco XYZ", "BANK");
    Institution result = institutionService.create(1L, request);

    assertThat(result.getName()).isEqualTo("Banco XYZ");
    assertThat(result.getType()).isEqualTo("BANK");
  }

  @Test
  void listReturnsInstitutions() {
    Institution institution = new Institution();
    institution.setName("Banco XYZ");
    when(institutionRepository.findAllByUserIdAndIsActiveTrue(1L)).thenReturn(List.of(institution));

    var result = institutionService.list(1L);

    assertThat(result).hasSize(1);
  }

  @Test
  void getReturnsInstitution() {
    Institution institution = new Institution();
    institution.setName("Banco XYZ");
    when(institutionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(institution));

    Institution result = institutionService.get(1L, 10L);

    assertThat(result.getName()).isEqualTo("Banco XYZ");
  }

  @Test
  void getThrowsWhenNotFound() {
    when(institutionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> institutionService.get(1L, 10L))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void updatePersistsChanges() {
    Institution institution = new Institution();
    institution.setName("Banco XYZ");
    when(institutionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(institution));
    when(institutionRepository.save(any(Institution.class))).thenAnswer(invocation -> invocation.getArgument(0));

    InstitutionRequest request = new InstitutionRequest("Banco ABC", "BROKER");
    Institution updated = institutionService.update(1L, 10L, request);

    assertThat(updated.getName()).isEqualTo("Banco ABC");
    assertThat(updated.getType()).isEqualTo("BROKER");
  }

  @Test
  void softDeleteSetsActiveToFalse() {
    Institution institution = new Institution();
    institution.setActive(true);
    when(institutionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(institution));
    when(institutionRepository.save(any(Institution.class))).thenAnswer(invocation -> invocation.getArgument(0));

    institutionService.softDelete(1L, 10L);

    assertThat(institution.isActive()).isFalse();
  }
}
