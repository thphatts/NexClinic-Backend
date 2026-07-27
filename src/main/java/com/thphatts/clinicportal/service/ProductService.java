package com.thphatts.clinicportal.service;

import com.thphatts.clinicportal.dto.response.PagedResponse;
import com.thphatts.clinicportal.dto.response.ProductResponse;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    PagedResponse<ProductResponse> getAllProducts(String name, String category, Pageable pageable);
}
