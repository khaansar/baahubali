package com.example.testservice.service;

import com.example.testservice.dto.CategoryDto;
import com.example.testservice.entity.Category;
import com.example.testservice.exception.ResourceNotFoundException;
import com.example.testservice.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    @CacheEvict(value = "baahubali:test:categories", allEntries = true)
    public CategoryDto createCategory(CategoryDto request, String adminId) {
        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setRequiredLanguages(request.getRequiredLanguages());
        category.setCreatedBy(adminId);
        category.setUpdatedBy(adminId);

        return mapToDto(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(value = "baahubali:test:categories", allEntries = true)
    public CategoryDto updateCategory(UUID id, CategoryDto request, String adminId) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setRequiredLanguages(request.getRequiredLanguages());
        category.setUpdatedBy(adminId);

        return mapToDto(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private CategoryDto mapToDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId() != null ? category.getId().toString() : null)
                .name(category.getName())
                .description(category.getDescription())
                .requiredLanguages(category.getRequiredLanguages())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .deletedAt(category.getDeletedAt())
                .build();
    }
}
