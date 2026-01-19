package com.christn.salesinventoryapi.service.iml;

import com.christn.salesinventoryapi.dto.mapper.ProductMapper;
import com.christn.salesinventoryapi.dto.request.ProductRequest;
import com.christn.salesinventoryapi.dto.response.ProductResponse;
import com.christn.salesinventoryapi.model.Category;
import com.christn.salesinventoryapi.model.Product;
import com.christn.salesinventoryapi.repository.CategoryRepository;
import com.christn.salesinventoryapi.repository.ProductRepository;
import com.christn.salesinventoryapi.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional()
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsByNameAndDeletedFalse(request.name())) {
            throw new IllegalArgumentException("Ya existe un producto con ese nombre");
        }

        Category category = categoryRepository.findByIdAndDeletedFalse(request.categoryId())
                .orElseThrow(() -> new EntityNotFoundException("Categoría no valida"));
        Product product = new Product();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setCategory(category);

        return ProductMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAllByDeletedFalse()
                .stream()
                .map(ProductMapper::toResponse)
                .toList();
    }

    @Override
    public List<ProductResponse> findAllByCategoryId(Long categoryId) {
        return productRepository.findAllByCategoryIdAndDeletedFalse(categoryId)
                .stream()
                .map(ProductMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));
        return ProductMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));

        if (!product.getName().equals(request.name()) && productRepository.existsByNameAndDeletedFalse(request.name())) {
            throw new IllegalArgumentException("Ya existe un producto con ese nombre");
        }

        Category category = categoryRepository.findByIdAndDeletedFalse(request.categoryId())
                .orElseThrow(() -> new EntityNotFoundException("Categoría no valida"));

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setCategory(category);

        return ProductMapper.toResponse(product);
    }

    //Soft delete
    @Override
    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));
        product.setDeleted(true);
    }
}
