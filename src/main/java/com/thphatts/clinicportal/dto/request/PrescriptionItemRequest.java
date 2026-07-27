package com.thphatts.clinicportal.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PrescriptionItemRequest(
        @NotNull(message = "Product ID không được để trống")
        Long productId,

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng thuốc phải lớn hơn 0")
        Integer quantity,

        @NotBlank(message = "Liều dùng không được để trống (Ví dụ: Ngày 2 lần, mỗi lần 1 viên)")
        String dosage
) {
}
