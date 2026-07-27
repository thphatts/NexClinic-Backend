package com.thphatts.clinicportal.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record DoctorRequest(
        @NotBlank(message = "Họ và tên bác sĩ không được để trống")
        @Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
        String fullName,

        @NotBlank(message = "Chuyên khoa không được để trống")
        String specialization,

        String degree,

        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Số điện thoại không hợp lệ")
        String phone,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        String email,

        @Min(value = 0, message = "Số năm kinh nghiệm không được âm")
        Integer experienceYears,

        @DecimalMin(value = "0.0", message = "Phí khám không được âm")
        BigDecimal consultationFee,

        String userId
) {}
