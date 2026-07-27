package com.thphatts.clinicportal.dto.response;

import java.util.List;

public record SymptomAnalysisResponse(
        String primaryDiagnosis,
        List<String> differentialDiagnoses,
        String riskLevel, // LOW, MEDIUM, HIGH, EMERGENCY
        String recommendedSpecialization,
        List<String> urgentWarnings,
        String advice,
        String disclaimer
) {
}
