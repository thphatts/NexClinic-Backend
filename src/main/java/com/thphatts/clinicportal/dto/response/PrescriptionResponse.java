package com.thphatts.clinicportal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PrescriptionResponse(
        Long id,
        String notes,
        BigDecimal totalAmount,
        List<PrescriptionItemResponse> items,
        LocalDateTime createdAt
) {
}
