package com.thphatts.clinicportal.modules.patient.dto;

import java.util.List;

public record MedicalRecordSummaryResponse(
        Long medicalRecordId,
        String patientName,
        String doctorName,
        String simplifiedSummary,
        List<String> keyPrecautions,
        String reexaminationAdvice,
        String disclaimer
) {
}
