package com.thphatts.clinicportal.service.impl;

import com.thphatts.clinicportal.dto.record.ProductResponse;
import com.thphatts.clinicportal.entity.Product;
import com.thphatts.clinicportal.mapper.ProductMapper;
import com.thphatts.clinicportal.repository.ProductRepository;
import com.thphatts.clinicportal.repository.specifiation.ProductSpecification;
import com.thphatts.clinicportal.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class IProductService implements ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    @Override
    public Page<ProductResponse> getAllProducts(String name, String category,Pageable pageable) {
       // Page<Product> products = productRepository.findAll(pageable);
        // Custom Query
       // Page<Product> products = productRepository.findActiveByCategory(category, pageable);
        //Jpa Specification
        Page<Product> products = productRepository.findAll(ProductSpecification.filterProducts(name, BigDecimal.valueOf(10000), category), pageable);

        return products.map(productMapper::toResponse);
    }
}
