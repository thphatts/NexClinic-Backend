package com.thphatts.clinicportal.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PrescriptionRequest(
        String notes,

        @NotEmpty(message = "Đơn thuốc phải có ít nhất 1 loại thuốc")
        List<@Valid PrescriptionItemRequest> items
) {
}
