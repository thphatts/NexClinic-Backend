package com.thphatts.clinicportal.modules.product.dto;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        BigDecimal price,
        String category

) {
}
