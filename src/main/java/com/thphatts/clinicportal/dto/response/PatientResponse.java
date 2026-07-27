package com.thphatts.clinicportal.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PatientResponse(
        Long id,
        String fullName,
        String citizenId,
        String phone,
        String email,
        LocalDate dob,
        String gender,
        String address,
        LocalDateTime createdAt,
        List<String> diagnoses
) {
}

