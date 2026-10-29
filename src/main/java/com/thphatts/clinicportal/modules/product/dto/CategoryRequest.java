package com.thphatts.clinicportal.modules.product.dto;

import com.thphatts.clinicportal.modules.product.dto.ProductRequest;

import java.util.List;

public record CategoryRequest(
        Long id,
        String name,
        List<ProductRequest> productRequestList
) {
}
