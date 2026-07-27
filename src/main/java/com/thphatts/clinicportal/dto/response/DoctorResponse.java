package com.thphatts.clinicportal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DoctorResponse(
        Long id,
        String fullName,
        String specialization,
        String degree,
        String phone,
        String email,
        Integer experienceYears,
        BigDecimal consultationFee,
        String userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
