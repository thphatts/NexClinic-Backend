package com.thphatts.clinicportal.controller;

import com.thphatts.clinicportal.common.ApiResponse;
import com.thphatts.clinicportal.common.BaseController;
import com.thphatts.clinicportal.dto.request.AiChatRequest;
import com.thphatts.clinicportal.dto.request.SymptomAnalysisRequest;
import com.thphatts.clinicportal.dto.response.AiChatResponse;
import com.thphatts.clinicportal.dto.response.MedicalRecordSummaryResponse;
import com.thphatts.clinicportal.dto.response.SymptomAnalysisResponse;
import com.thphatts.clinicportal.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController extends BaseController {

    private final AiService aiService;

    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        return ApiResponse.success(aiService.chat(request));
    }

    @PostMapping("/analyze-symptoms")
    public ApiResponse<SymptomAnalysisResponse> analyzeSymptoms(@Valid @RequestBody SymptomAnalysisRequest request) {
        return ApiResponse.success(aiService.analyzeSymptoms(request));
    }

    @PostMapping("/summarize-medical-record/{medicalRecordId}")
    public ApiResponse<MedicalRecordSummaryResponse> summarizeMedicalRecord(@PathVariable("medicalRecordId") Long medicalRecordId) {
        return ApiResponse.success(aiService.summarizeMedicalRecord(medicalRecordId));
    }
}
