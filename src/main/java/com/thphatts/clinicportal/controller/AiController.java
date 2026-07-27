package com.thphatts.clinicportal.controller;

import com.thphatts.clinicportal.dto.request.AiChatRequest;
import com.thphatts.clinicportal.dto.request.SymptomAnalysisRequest;
import com.thphatts.clinicportal.dto.response.AiChatResponse;
import com.thphatts.clinicportal.dto.response.MedicalRecordSummaryResponse;
import com.thphatts.clinicportal.dto.response.SymptomAnalysisResponse;
import com.thphatts.clinicportal.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        AiChatResponse response = aiService.chat(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @RequestParam String message,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String context
    ) {
        return aiService.streamChat(new AiChatRequest(message, sessionId, context));
    }

    @PostMapping("/analyze-symptoms")
    public ResponseEntity<SymptomAnalysisResponse> analyzeSymptoms(@Valid @RequestBody SymptomAnalysisRequest request) {
        SymptomAnalysisResponse response = aiService.analyzeSymptoms(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/summarize-medical-record/{id}")
    public ResponseEntity<MedicalRecordSummaryResponse> summarizeMedicalRecord(@PathVariable Long id) {
        MedicalRecordSummaryResponse response = aiService.summarizeMedicalRecord(id);
        return ResponseEntity.ok(response);
    }
}
