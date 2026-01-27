package com.moneta.category;

import com.moneta.auth.User;
import com.moneta.auth.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {
  private final CategoryRepository categoryRepository;
  private final UserRepository userRepository;

  public CategoryService(CategoryRepository categoryRepository, UserRepository userRepository) {
    this.categoryRepository = categoryRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public Category create(Long userId, CategoryDtos.CategoryRequest request) {
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("usuário não encontrado"));
    Category category = new Category();
    category.setUser(user);
    category.setName(request.name());
    category.setColor(request.color());
    return categoryRepository.save(category);
  }

  public List<Category> list(Long userId) {
    return categoryRepository.findAllByUserIdAndIsActiveTrue(userId);
  }

  public Category get(Long userId, Long id) {
    return categoryRepository.findByIdAndUserId(id, userId)
      .orElseThrow(() -> new IllegalArgumentException("categoria não encontrada"));
  }

  @Transactional
  public Category update(Long userId, Long id, CategoryDtos.CategoryRequest request) {
    Category category = get(userId, id);
    category.setName(request.name());
    category.setColor(request.color());
    return categoryRepository.save(category);
  }

  @Transactional
  public void softDelete(Long userId, Long id) {
    Category category = get(userId, id);
    category.setActive(false);
    categoryRepository.save(category);
  }
}
