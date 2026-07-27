package com.thphatts.clinicportal.service;

import com.thphatts.clinicportal.dto.request.AiChatRequest;
import com.thphatts.clinicportal.dto.request.SymptomAnalysisRequest;
import com.thphatts.clinicportal.dto.response.AiChatResponse;
import com.thphatts.clinicportal.dto.response.MedicalRecordSummaryResponse;
import com.thphatts.clinicportal.dto.response.SymptomAnalysisResponse;

public interface AiService {

    AiChatResponse chat(AiChatRequest request);

    SymptomAnalysisResponse analyzeSymptoms(SymptomAnalysisRequest request);

    MedicalRecordSummaryResponse summarizeMedicalRecord(Long medicalRecordId);
}
