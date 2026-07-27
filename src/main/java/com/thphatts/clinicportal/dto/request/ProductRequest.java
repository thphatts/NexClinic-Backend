package com.thphatts.clinicportal.dto.request;

import java.math.BigDecimal;

public record ProductRequest(
        Long id,
        String name,
        BigDecimal price
) {
}
