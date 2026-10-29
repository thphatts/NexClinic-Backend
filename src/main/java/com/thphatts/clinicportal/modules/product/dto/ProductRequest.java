package com.thphatts.clinicportal.modules.product.dto;

import java.math.BigDecimal;

public record ProductRequest(
        Long id,
        String name,
        BigDecimal price
) {
}
