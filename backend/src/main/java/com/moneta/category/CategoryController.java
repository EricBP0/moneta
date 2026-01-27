package com.moneta.category;

import com.moneta.category.CategoryDtos.CategoryRequest;
import com.moneta.category.CategoryDtos.CategoryResponse;
import com.moneta.config.UserPrincipal;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
  private final CategoryService categoryService;

  public CategoryController(CategoryService categoryService) {
    this.categoryService = categoryService;
  }

  @GetMapping
  public List<CategoryResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
    return categoryService.list(principal.getId()).stream()
      .map(this::toResponse)
      .toList();
  }

  @PostMapping
  public CategoryResponse create(
    @AuthenticationPrincipal UserPrincipal principal,
    @Valid @RequestBody CategoryRequest request
  ) {
    return toResponse(categoryService.create(principal.getId(), request));
  }

  @PatchMapping("/{id}")
  public CategoryResponse update(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id,
    @Valid @RequestBody CategoryRequest request
  ) {
    return toResponse(categoryService.update(principal.getId(), id, request));
  }

  @DeleteMapping("/{id}")
  public void delete(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id
  ) {
    categoryService.softDelete(principal.getId(), id);
  }

  private CategoryResponse toResponse(Category category) {
    return new CategoryResponse(
      category.getId(),
      category.getName(),
      category.getColor(),
      category.isActive()
    );
  }
}
