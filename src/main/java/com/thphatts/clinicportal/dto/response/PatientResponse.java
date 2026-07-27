package com.thphatts.clinicportal.dto.response;

import java.util.List;

public record PatientResponse(
        Long id,
        String fullName,
        List<String> diagnoses
) {
}
