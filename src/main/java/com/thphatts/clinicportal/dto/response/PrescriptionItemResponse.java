package com.thphatts.clinicportal.dto.response;

import java.math.BigDecimal;

public record PrescriptionItemResponse(
        Long id,
        Long productId,
        String productName,
        Integer quantity,
        String dosage,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
}
