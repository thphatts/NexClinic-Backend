package com.thphatts.clinicportal.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SymptomAnalysisRequest(
        @NotBlank(message = "Mô tả triệu chứng không được để trống")
        String symptoms,

        @Min(value = 1, message = "Số ngày bị bệnh phải từ 1 ngày trở lên")
        Integer durationDays,

        Integer patientAge,

        String medicalHistory
) {
}
