package com.christn.salesinventoryapi.service.iml;

import com.christn.salesinventoryapi.dto.mapper.CategoryMapper;
import com.christn.salesinventoryapi.dto.request.CategoryRequest;
import com.christn.salesinventoryapi.dto.response.CategoryResponse;
import com.christn.salesinventoryapi.model.Category;
import com.christn.salesinventoryapi.repository.CategoryRepository;
import com.christn.salesinventoryapi.service.CategoryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository repository;

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (repository.existsByNameAndDeletedFalse(request.name())) {
            throw new IllegalStateException("Ya existe una categoría con ese nombre");
        }

        Category category = new Category();
        category.setName(request.name());
        category.setDescription(request.description());

        return CategoryMapper.toResponse(repository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return repository.findAllByDeletedFalse()
                .stream()
                .map(CategoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        Category category = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada"));
        return CategoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada"));

        if (!category.getName().equals(request.name()) && repository.existsByNameAndDeletedFalse(request.name())) {
            throw new IllegalStateException("Ya existe una categoría con ese nombre");
        }

        category.setName(request.name());
        category.setDescription(request.description());

        return CategoryMapper.toResponse(category);
    }

    //Soft delete
    @Override
    @Transactional
    public void delete(Long id) {
        Category category = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada"));
        category.setDeleted(true);
    }
}
