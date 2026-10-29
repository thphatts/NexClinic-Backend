package com.thphatts.clinicportal.modules.product.service;

import com.thphatts.clinicportal.common.dto.PagedResponse;
import com.thphatts.clinicportal.modules.product.dto.ProductResponse;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    PagedResponse<ProductResponse> getAllProducts(String name, String category, Pageable pageable);
}
