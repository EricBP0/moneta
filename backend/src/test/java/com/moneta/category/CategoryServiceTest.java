package com.moneta.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.moneta.auth.UserRepository;
import com.moneta.category.CategoryDtos.CategoryRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
  @Mock
  private CategoryRepository categoryRepository;

  @Mock
  private UserRepository userRepository;

  private CategoryService categoryService;

  @BeforeEach
  void setup() {
    categoryService = new CategoryService(categoryRepository, userRepository);
  }

  @Test
  void createRequiresUser() {
    when(userRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> categoryService.create(1L, new CategoryRequest("Categoria", null)))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void listReturnsCategories() {
    Category category = new Category();
    category.setName("Categoria");
    when(categoryRepository.findAllByUserIdAndIsActiveTrue(1L)).thenReturn(List.of(category));

    var result = categoryService.list(1L);

    assertThat(result).hasSize(1);
  }

  @Test
  void updatePersistsChanges() {
    Category category = new Category();
    category.setName("Categoria");
    when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(category));
    when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

    CategoryRequest request = new CategoryRequest("Nova", "#fff");
    Category updated = categoryService.update(1L, 10L, request);

    assertThat(updated.getName()).isEqualTo("Nova");
  }
}
