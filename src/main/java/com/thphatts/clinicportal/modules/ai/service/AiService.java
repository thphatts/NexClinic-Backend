package com.thphatts.clinicportal.modules.ai.service;

import com.thphatts.clinicportal.modules.ai.dto.AiChatRequest;
import com.thphatts.clinicportal.modules.ai.dto.SymptomAnalysisRequest;
import com.thphatts.clinicportal.modules.ai.dto.AiChatResponse;
import com.thphatts.clinicportal.modules.patient.dto.MedicalRecordSummaryResponse;
import com.thphatts.clinicportal.modules.ai.dto.SymptomAnalysisResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiService {

    /**
     * Tư vấn trực tuyến qua AI Assistant (Synchronous Response)
     */
    AiChatResponse chat(AiChatRequest request);

    /**
     * Tư vấn trực tuyến qua AI Assistant dạng Realtime Stream (Server-Sent Events)
     */
    SseEmitter streamChat(AiChatRequest request);

    /**
     * Chẩn đoán triệu chứng & Phân loại mức độ rủi ro y tế
     */
    SymptomAnalysisResponse analyzeSymptoms(SymptomAnalysisRequest request);

    /**
     * Tóm tắt hồ sơ bệnh án cho bệnh nhân bằng ngôn ngữ bình dân
     */
    MedicalRecordSummaryResponse summarizeMedicalRecord(Long medicalRecordId);
}
