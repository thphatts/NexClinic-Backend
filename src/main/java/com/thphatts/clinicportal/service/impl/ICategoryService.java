package com.thphatts.clinicportal.service.impl;

import com.thphatts.clinicportal.dto.request.CategoryRequest;
import com.thphatts.clinicportal.dto.request.ProductRequest;
import com.thphatts.clinicportal.entity.Category;
import com.thphatts.clinicportal.entity.Product;
import com.thphatts.clinicportal.mapper.CategoryMapper;
import com.thphatts.clinicportal.repository.CategoryRepository;
import com.thphatts.clinicportal.repository.ProductRepository;
import com.thphatts.clinicportal.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ICategoryService implements CategoryService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    @Override
    public Category createCategory(CategoryRequest categoryRequest) {
        Category category = categoryMapper.toEntity(categoryRequest);

        if(Objects.nonNull(categoryRequest.productRequestList())) {
            for (ProductRequest product : categoryRequest.productRequestList()) {
                Product pro = categoryMapper.toProductEntity(product);
                category.addProduct(pro);
            }
        }
        return categoryRepository.save(category);
    }

    @Override
    public Category updateCategory(Long categoryID, CategoryRequest categoryRequest) {
        Category category = categoryRepository.findById(categoryID).orElseThrow(() -> new RuntimeException("category not found!"));

        category.setName(categoryRequest.name());

        List<Long> newProductId = Objects.nonNull(categoryRequest.productRequestList()) ? categoryRequest.productRequestList().stream().map(ProductRequest::id).filter(Objects::nonNull).toList() : List.of();
        category.getProducts().removeIf(product -> !newProductId.contains(product.getId()));
        if(Objects.nonNull(categoryRequest.productRequestList())){
            for(ProductRequest productRequest : categoryRequest.productRequestList()) {
                if (productRequest.id() == null) {
                    Product newProduct = categoryMapper.toProductEntity(productRequest);
                    category.addProduct(newProduct);
                }
                else {
                        Product existing = category.getProducts()
                                .stream()
                                .filter(product -> product.getId().equals(productRequest.id()))
                                .findFirst()
                                .orElse(null);
                        if(existing != null){
                            existing.setName(productRequest.name());
                            existing.setPrice(productRequest.price());
                        }
                }
            }
        }
        return categoryRepository.save(category);
    }
}
