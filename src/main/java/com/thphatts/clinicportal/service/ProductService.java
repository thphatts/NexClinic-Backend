package com.thphatts.clinicportal.service;

import com.thphatts.clinicportal.dto.record.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    Page<ProductResponse> getAllProducts(String name, String category,Pageable pageable);
}
